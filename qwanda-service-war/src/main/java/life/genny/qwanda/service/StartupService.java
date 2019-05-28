package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import org.jboss.ejb3.annotation.TransactionTimeout;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeDate;
import life.genny.qwanda.attribute.AttributeInteger;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.SearchEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwandautils.JsonUtils;
import life.genny.services.BatchLoading;
import life.genny.services.ProjectsLoading;

import life.genny.eventbus.EventBusInterface;
import io.vertx.resourceadapter.examples.mdb.EventBusBean;
import io.vertx.resourceadapter.examples.mdb.WildflyCache;
import javax.inject.Inject;
import life.genny.utils.VertxUtils;
import life.genny.qwandautils.GennySettings;

import life.genny.qwanda.message.QDataSubLayoutMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;

import life.genny.qwandautils.GitUtils;
import life.genny.qwanda.Answer;
import life.genny.qwanda.GPSLocation;
import life.genny.qwanda.GPSRoute;
import life.genny.qwanda.GPSRouteStatus;
import life.genny.qwanda.Layout;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.controller.Controller;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QDataSubLayoutMessage;
import life.genny.qwanda.message.QEventSystemMessage;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;
import life.genny.qwandautils.GPSUtils;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.GitUtils;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.Optional;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import java.io.File;
import java.util.Map;

import life.genny.security.SecureResources;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.cfg.Configuration;
import org.hibernate.criterion.Restrictions;

/**
 * This Service bean Starts up the main API service. Loading in the
 * database/google bootstrap/github layouts
 *
 * @author Adam Crow
 */
@Singleton
@Startup


//@TransactionTimeout(value = 8000, unit = TimeUnit.SECONDS)
public class StartupService {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Inject
	Hazel inDb;

	@Inject
	EventBusBean eventBus;

	@Inject
	private Service service;

	@Inject
	private SecurityService securityService;

	@Inject
	private ServiceTokenService serviceTokens;

	WildflyCache cacheInterface;

	@PersistenceContext
	private EntityManager em;

	@PostConstruct
	@Transactional
	public void init() {

		cacheInterface = new WildflyCache(inDb);

		VertxUtils.init(eventBus, cacheInterface);

		securityService.setImportMode(true); // ugly way of getting past security

		String secret = System.getenv("GOOGLE_CLIENT_SECRET");
		String hostingSheetId = System.getenv("GOOGLE_HOSTING_SHEET_ID");
		File credentialPath = new File(System.getProperty("user.home"), ".genny/sheets.googleapis.com-java-quickstart");

		Map<String, Map> projects = ProjectsLoading.loadIntoMap(hostingSheetId, secret, credentialPath);

		serviceTokens.init(projects);

		// Save projects
		saveProjectBes(projects);
		pushProjectsUrlsToDTT(projects);

		if (!GennySettings.skipGoogleDocInStartup) {
			log.info("Starting Transaction for loading");

			for (String projectCode : projects.keySet()) {
				log.info("Project: " + projects.get(projectCode));
				Map<String, Object> project = projects.get(projectCode);
				if ("FALSE".equals((String) project.get("disable"))) {
					String realm = ((String) project.get("code"));
					service.setCurrentRealm(realm);
					log.info("PROJECT " + realm);
					BatchLoading bl = new BatchLoading(project, service);

					// save urls to Keycloak maps
					service.setCurrentRealm(projectCode); // provide overridden realm

					bl.persistProject(false, null, false);
					String keycloakJson = bl.constructKeycloakJson(project);
					bl.upsertKeycloakJson(keycloakJson);
			//		bl.upsertProjectUrls((String) project.get("urlList"));
				}
			}
			log.info("*********************** Finished Google Doc Import ***********************************");
		} else {
			log.info("Skipping Google doc loading");
		}

		// Push BEs to cache
		if (System.getenv("LOAD_DDT_IN_STARTUP") != null) {
			pushToDTT();
		}

		pushProjectsUrlsToDTT(projects);

		log.info("skipGithubInStartup is " + (GennySettings.skipGithubInStartup ? "TRUE" : "FALSE"));
		String branch = "master";
		if (!GennySettings.skipGithubInStartup) {

			log.info("************* Generating V1 Layouts *************");
			for (String realm : projects.keySet()) {
				Map<String, Object> project = projects.get(realm);
				if ("FALSE".equals((String) project.get("disable"))) {

					try {
						List<BaseEntity> v1GennyLayouts = GitUtils.getLayoutBaseEntitys(GennySettings.githubLayoutsUrl,
								branch, realm, "genny/sublayouts", true); // get common layouts
						saveLayouts(realm, v1GennyLayouts);
						List<BaseEntity> v1RealmLayouts = GitUtils.getLayoutBaseEntitys(GennySettings.githubLayoutsUrl,
								branch, realm, realm + "/sublayouts", true);
						saveLayouts(realm, v1RealmLayouts);

						log.info("************* Generating V2 Layouts *************");
						List<BaseEntity> v2GennyLayouts = GitUtils.getLayoutBaseEntitys(GennySettings.githubLayoutsUrl,
								branch, realm, "genny", false); // get common layouts
						saveLayouts(realm, v2GennyLayouts);
						List<BaseEntity> v2RealmLayouts = GitUtils.getLayoutBaseEntitys(GennySettings.githubLayoutsUrl,
								branch, realm, realm + "-new", true);
						saveLayouts(realm, v2RealmLayouts);
					} catch (Exception e) {
						log.error("Bad Data Exception");
					}
				}
			}
		} else {
			log.info("Skipped Github Layout loading ....");
		}

		log.info("Loading V1 Genny Sublayouts");
		for (String realm : projects.keySet()) {
			Map<String, Object> project = projects.get(realm);
			if ("FALSE".equals((String) project.get("disable"))) {

				QDataSubLayoutMessage sublayoutsMsg = service.fetchSubLayoutsFromDb(realm, "genny/sublayouts",
						"master");
				log.info("Loaded " + sublayoutsMsg.getItems().length + " V1 " + realm + " Realm Sublayouts");

				VertxUtils.writeCachedJson(realm, "GENNY-V1-LAYOUTS", JsonUtils.toJson(sublayoutsMsg),
						serviceTokens.getServiceToken(realm));

				log.info("Loading V1 " + realm + " Realm Sublayouts");
				sublayoutsMsg = service.fetchSubLayoutsFromDb(realm, realm + "/sublayouts", "master");
				log.info("Loaded " + sublayoutsMsg.getItems().length + " V1 " + realm + " Realm Sublayouts");
				VertxUtils.writeCachedJson(realm, realm.toUpperCase() + "-V1-LAYOUTS", JsonUtils.toJson(sublayoutsMsg),
						serviceTokens.getServiceToken(realm));

				SearchEntity searchBE = new SearchEntity("SER_V2_LAYOUTS", "V2 Layout Search");
				try {
					searchBE.setValue(new AttributeInteger("SCH_PAGE_START", "PageStart"), 0);
					searchBE.setValue(new AttributeInteger("SCH_PAGE_SIZE", "PageSize"), 1000);
					searchBE.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "LAY_%");
					// searchBE.addFilter("PRI_BRANCH",SearchEntity.StringFilter.LIKE, branch);

				} catch (BadDataException e) {
					log.error("Bad Data Exception");
				}

				List<BaseEntity> v2layouts = service.findBySearchBE(searchBE);

				log.info("Loaded " + v2layouts.size() + " V2 " + realm + "-new Realm Sublayouts");

				QDataBaseEntityMessage msg = new QDataBaseEntityMessage(v2layouts.toArray(new BaseEntity[0]));
				msg.setParentCode("GRP_LAYOUTS");
				msg.setLinkCode("LNK_CORE");
				VertxUtils.writeCachedJson(realm, "V2-LAYOUTS", JsonUtils.toJson(msg),
						serviceTokens.getServiceToken(realm));
			}
		}

		for (String realm : projects.keySet()) {
			Map<String, Object> project = projects.get(realm);
			if ("FALSE".equals((String) project.get("disable"))) {

				String accessToken = serviceTokens.getServiceToken(realm);
				service.sendQEventSystemMessage("EVT_QWANDA_SERVICE_STARTED", accessToken);
			}
		}

		log.info("---------------- Completed Startup ----------------");
		securityService.setImportMode(false);

	}

	public void pushToDTT() {
		// Attributes
		log.info("Pushing Attributes to Cache");
		final List<Attribute> entitys = service.findAttributes();
		Attribute[] atArr = new Attribute[entitys.size()];
		atArr = entitys.toArray(atArr);
		QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
		String json = JsonUtils.toJson(msg);
		service.writeToDDT("attributes", json);
		log.info("Pushed " + entitys.size() + " attributes to cache");

		// BaseEntitys
		List<BaseEntity> results = em
				.createQuery("SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea ").getResultList();

//		List<BaseEntity> results = em
//				.createQuery("SELECT be FROM BaseEntity be  JOIN  be.baseEntityAttributes ea ").getResultList();

		// Collect all the baseentitys
		log.info("Pushing " + results.size() + " Basentitys to Cache");
		service.writeToDDT(results);
		log.info("Pushed " + results.size() + " Basentitys to Cache");

	}

	// The following function is also in the serviceEndpoint. It is here because
	// hibernate is not letting me save easily
	public void saveLayouts(final String realm, final List<BaseEntity> layouts) {
		service.setCurrentRealm(realm);
		Layout[] layoutArray = new Layout[layouts.size()];

		Attribute layoutDataAttribute = service.findAttributeByCode("PRI_LAYOUT_DATA");
		Attribute layoutURLAttribute = service.findAttributeByCode("PRI_LAYOUT_URL");
		Attribute layoutURIAttribute = service.findAttributeByCode("PRI_LAYOUT_URI");
		Attribute layoutNameAttribute = service.findAttributeByCode("PRI_LAYOUT_NAME");
		Attribute layoutModifiedDateAttribute = service.findAttributeByCode("PRI_LAYOUT_MODIFIED_DATE");

		int index = 0;
		for (BaseEntity layout : layouts) {
			final String code = layout.getCode();
			BaseEntity existingLayout = null;
			BaseEntity newLayout = null;
			try {
				newLayout = service.findBaseEntityByCode(layout.getCode(), true);
			} catch (NoResultException e) {
				log.info("New Layout detected");
			}
			if (newLayout == null) {
				newLayout = new BaseEntity(layout.getCode(), layout.getName());
			} else {
				int newData = layout.getValue("PRI_LAYOUT_DATA").get().toString().hashCode();
				Optional<EntityAttribute> oldDataEA = newLayout.findEntityAttribute("PRI_LAYOUT_DATA");
				if (oldDataEA.isPresent()) {
					int oldData = oldDataEA.get().getAsString().hashCode();
					if (newData == oldData) {
						continue;
					}
				}
			}
			newLayout.setRealm(realm);
			newLayout.setUpdated(layout.getUpdated());

			service.upsert(newLayout);

			try {
				EntityAttribute ea = newLayout.addAttribute(layoutDataAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_DATA").get().toString());
				EntityAttribute ea2 = newLayout.addAttribute(layoutURLAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_URL").get().toString());
				EntityAttribute ea3 = newLayout.addAttribute(layoutURIAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_URI").get().toString());
				EntityAttribute ea4 = newLayout.addAttribute(layoutNameAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_NAME").get().toString());
				EntityAttribute ea5 = newLayout.addAttribute(layoutModifiedDateAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_MODIFIED_DATE").get().toString());
			} catch (final BadDataException e) {
				e.printStackTrace();
			}

			try {

				try {
					// merge in entityAttributes
					newLayout = service.getEntityManager().merge(newLayout);
				} catch (final Exception e) {
					// so persist otherwise
					service.getEntityManager().persist(newLayout);
				}
				service.addLink("GRP_LAYOUTS", newLayout.getCode(), "LNK_CORE", "LAYOUT", 1.0, false); // don't send
																										// change event
			} catch (IllegalArgumentException | BadDataException e) {
				log.error("Could not write layout - " + e.getLocalizedMessage());
			}

		}
	}


	private void saveProjectBes(Map<String, Map> projects) {
		log.info("Updating Project BaseEntitys ");

		for (String realmCode : projects.keySet()) {
			Map<String, Object> project = projects.get(realmCode);
			if ("FALSE".equals((String) project.get("disable"))) {

				service.setCurrentRealm(realmCode);
				log.info("Project: " + projects.get(realmCode));

				if ("FALSE".equals((String) project.get("disable"))) {
					String realm = realmCode;
					String keycloakUrl = (String) project.get("keycloakUrl");
					String name = (String) project.get("name");
					String sheetID = (String) project.get("sheetID");
					String urlList = (String) project.get("urlList");
					String code = (String) project.get("code");
					String disable = (String) project.get("disable");
					String secret = (String) project.get("clientSecret");
					String key = (String) project.get("ENV_SECURITY_KEY");
					String encryptedPassword = (String) project.get("ENV_SERVICE_PASSWORD");
					String realmToken = serviceTokens.getServiceToken(realm);

					String projectCode = "PRJ_" + realm.toUpperCase();
					BaseEntity projectBe = null;
	
						projectBe = new BaseEntity(projectCode, name);
						projectBe = service.upsert(projectBe);


					projectBe = createAnswer(projectBe, "PRI_NAME", name, false);
					projectBe = createAnswer(projectBe, "PRI_CODE", projectCode, false);
					projectBe = createAnswer(projectBe, "ENV_SECURITY_KEY", key, true);
					projectBe = createAnswer(projectBe, "ENV_SERVICE_PASSWORD", encryptedPassword, true);
					projectBe = createAnswer(projectBe, "ENV_SERVICE_TOKEN", realmToken, true);
					projectBe = createAnswer(projectBe, "ENV_SECRET", secret, true);
					projectBe = createAnswer(projectBe, "ENV_SHEET_ID", sheetID, true);
					projectBe = createAnswer(projectBe, "ENV_URL_LIST", urlList, true);
					projectBe = createAnswer(projectBe, "ENV_DISABLE", disable, true);
					projectBe = createAnswer(projectBe, "ENV_REALM", realm, true);
					projectBe = createAnswer(projectBe, "ENV_KEYCLOAK_URL", keycloakUrl, true);

					BatchLoading bl = new BatchLoading(project, service);
					String keycloakJson = bl.constructKeycloakJson(project);
					projectBe = createAnswer(projectBe, "ENV_KEYCLOAK_JSON", keycloakJson, true);

					projectBe = service.upsert(projectBe);

					// Set up temp keycloak.json Maps
					String[] urls = urlList.split(",");
					SecureResources.addRealm(realm, keycloakJson);
					SecureResources.addRealm(realm + ".json", keycloakJson);
					// redundant
					if (("genny".equals(realm))) {
						SecureResources.addRealm("genny", keycloakJson);
						SecureResources.addRealm("genny.json", keycloakJson);
						SecureResources.addRealm("qwanda-service.genny.life.json", keycloakJson);
					}

					// Overwrite all the time, must have localhost
					SecureResources.addRealm("localhost.json", keycloakJson);
					SecureResources.addRealm("localhost", keycloakJson);
					SecureResources.addRealm("localhost:8080", keycloakJson);
					for (String url : urls) {
						SecureResources.addRealm(url + ".json", keycloakJson);
						SecureResources.addRealm(url, keycloakJson);
					}

				}
			}
		}

	}


	private BaseEntity createAnswer(BaseEntity be, final String attributeCode, final String answerValue,
			final Boolean privacy) {
		try {
			Answer answer = null;
			Attribute attribute = null;
			try {
				attribute = service.findAttributeByCode(attributeCode);
			} catch (Exception ee) {
				// Could not find Attribute, create it
				attribute = new Attribute(attributeCode, attributeCode, new DataType("DTT_TEXT"));
				service.insert(attribute);
			}
			if (attribute == null) {
				attribute = new Attribute(attributeCode, attributeCode, new DataType("DTT_TEXT"));
				service.insert(attribute);
			}
			answer = new Answer(be, be, attribute, answerValue);
			answer.setChangeEvent(false);
			be.addAnswer(answer);
			EntityAttribute ea = be.findEntityAttribute(attribute);
			ea.setPrivacyFlag(privacy);
			ea.setRealm(be.getRealm());
		} catch (Exception e) {
			log.error("CANNOT UPDATE PROJECT " + be.getCode() + " " + e.getLocalizedMessage());
		}
		return be;

	}


	private void pushProjectsUrlsToDTT(Map<String, Map> projects) {

		final List<String> realms = service.getRealms();

		List<String> activeRealms = new ArrayList<String>(); // build up the active realms to put into a single location
																// in the cache

		for (String realm : realms) {
			Map<String, Object> project = projects.get(realm);
			if ("FALSE".equals((String) project.get("disable"))) {

				// push the project to the urls as keys too
				service.setCurrentRealm(realm);
				activeRealms.add(realm);

				BaseEntity be = null; //service.findBaseEntityByCode("PRJ_" + realm.toUpperCase(), true);
				CriteriaBuilder cb = em.getCriteriaBuilder();
			    CriteriaQuery<BaseEntity> query = cb.createQuery(BaseEntity.class);
			    Root<BaseEntity> root = query.from(BaseEntity.class);

			    query = query.select(root)
			            .where(cb.equal(root.get("code"), "PRJ_" + realm.toUpperCase()),
			                    cb.equal(root.get("realm"), realm));
			    
			    try {
			        be = em.createQuery(query).getSingleResult();
			    } catch (NoResultException nre) {
			       
			    }
//				Session session = em.unwrap(org.hibernate.Session.class);
//				Criteria criteria = session.createCriteria(BaseEntity.class);
//				BaseEntity be = (BaseEntity)criteria
//						.add(Restrictions.eq("code", projectBe.getCode()))
//						.add(Restrictions.eq("realm", projectBe.getRealm()))
//				                             .uniqueResult();

				String urlList = be.getValue("ENV_URL_LIST", "alyson3.genny.life");
				String token = serviceTokens.getServiceToken(realm); //be.getValue("ENV_SERVICE_TOKEN", "DUMMY");

				log.info(be.getRealm() + ":" + be.getCode() + ":token=" + token);
				VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase(), token);
				VertxUtils.putObject(realm, "CACHE", "SERVICE_TOKEN", token);
				String[] urls = urlList.split(",");
				for (String url : urls) {
					VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, url.toUpperCase(), JsonUtils.toJson(be));
					VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "TOKEN" + url.toUpperCase(), token);
				}
			}
			//
		}
		// Push the list of active realms
		Type listType = new TypeToken<List<String>>() {
		}.getType();
		Gson gson = new Gson();
		String realmsJson = JsonUtils.toJson(activeRealms);
		VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "REALMS", realmsJson);
	}
}
