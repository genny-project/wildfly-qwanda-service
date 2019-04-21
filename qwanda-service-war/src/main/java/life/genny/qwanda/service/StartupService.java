package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;
import java.time.LocalDate;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import org.jboss.ejb3.annotation.TransactionTimeout;
import java.util.concurrent.TimeUnit;
import org.apache.logging.log4j.Logger;

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
 * This Service bean Starts up the main API service. Loading in the database/google bootstrap/github layouts
 *
 * @author Adam Crow
 */
@Singleton
@Startup
@Transactional

@TransactionTimeout(value = 8000, unit = TimeUnit.SECONDS)
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
		File credentialPath = new File(System.getProperty("user.home"),
				".genny/sheets.googleapis.com-java-quickstart");

		Map<String,Map> projects = ProjectsLoading.loadIntoMap(hostingSheetId, secret, credentialPath);
		
		serviceTokens.init(projects);
		
		// Save projects
		saveProjectBes(projects, service);
		pushProjectsUrlsToDTT();

		if (!GennySettings.skipGoogleDocInStartup) {
			log.info("Starting Transaction for loading");
			
			
			for (String projectCode : projects.keySet()) {
				log.info("Project: "+projects.get(projectCode));
				Map<String,Object> project = projects.get(projectCode);
				if ("FALSE".equals((String)project.get("disable"))) {
					String realm = ((String)project.get("code"));
					service.setCurrentRealm(realm);
					log.info("PROJECT "+realm);
					BatchLoading bl = new BatchLoading(project,service);
					

					// Set up temp keycloak.json Maps
	                String keycloakJson = bl.constructKeycloakJson(project);
	                String urlList = ((String)project.get("urlList"));
	    			String[] urls = urlList.split(",");
	    			SecureResources.getKeycloakJsonMap().put(realm,keycloakJson);
	    			SecureResources.getKeycloakJsonMap().put(realm+".json",keycloakJson);
	    			// redundant
	    			SecureResources.getKeycloakJsonMap().put("genny",keycloakJson);
	    			SecureResources.getKeycloakJsonMap().put("genny.json",keycloakJson);
	    			for (String url : urls) {
	    			 	SecureResources.getKeycloakJsonMap().put(url+".json",keycloakJson);
	    				SecureResources.getKeycloakJsonMap().put(url,keycloakJson);
	    				if ("http://alyson.genny.life".equalsIgnoreCase(url)) {
	    					SecureResources.getKeycloakJsonMap().put("localhost.json",keycloakJson);
	    					SecureResources.getKeycloakJsonMap().put("localhost",keycloakJson);
	    				}
	    			}

					
					// save urls to Keycloak maps
					service.setCurrentRealm(projectCode);   // provide overridden realm
					
					bl.persistProject(false, null, false);
				
	                bl.upsertKeycloakJson(keycloakJson);
	                bl.upsertProjectUrls((String)project.get("urlList"));
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
		
		pushProjectsUrlsToDTT();

		String accessToken = service.getServiceToken(GennySettings.mainrealm);
		log.info("ACCESS_TOKEN: " + accessToken);
		service.sendQEventSystemMessage("EVT_QWANDA_SERVICE_STARTED", accessToken);

		log.info("skipGithubInStartup is " + (GennySettings.skipGithubInStartup ? "TRUE" : "FALSE"));
		String branch = "master";
		if (!GennySettings.skipGithubInStartup) {

			log.info("************* Generating V1 Layouts *************");
			try {
				List<BaseEntity> v1GennyLayouts = GitUtils.getLayoutBaseEntitys(GennySettings.githubLayoutsUrl, branch,
						GennySettings.mainrealm, "genny/sublayouts", true); // get common layouts
				saveLayouts(v1GennyLayouts);
				List<BaseEntity> v1RealmLayouts = GitUtils.getLayoutBaseEntitys(GennySettings.githubLayoutsUrl, branch,
						GennySettings.mainrealm, GennySettings.mainrealm + "/sublayouts", true);
				saveLayouts(v1RealmLayouts);

				log.info("************* Generating V2 Layouts *************");
				List<BaseEntity> v2GennyLayouts = GitUtils.getLayoutBaseEntitys(GennySettings.githubLayoutsUrl, branch,
						GennySettings.mainrealm, "genny", false); // get common layouts
				saveLayouts(v2GennyLayouts);
				List<BaseEntity> v2RealmLayouts = GitUtils.getLayoutBaseEntitys(GennySettings.githubLayoutsUrl, branch,
						GennySettings.mainrealm, GennySettings.mainrealm + "-new", true);
				saveLayouts(v2RealmLayouts);
			} catch (Exception e) {
				log.error("Bad Data Exception");
			}
		} else {
			log.info("Skipped Github Layout loading ....");
		}

		log.info("Loading V1 Genny Sublayouts");
		QDataSubLayoutMessage sublayoutsMsg = service.fetchSubLayoutsFromDb(GennySettings.mainrealm, "genny/sublayouts",
				"master");
		log.info("Loaded " + sublayoutsMsg.getItems().length + " V1 " + GennySettings.mainrealm + " Realm Sublayouts");

		VertxUtils.writeCachedJson(GennySettings.mainrealm, "GENNY-V1-LAYOUTS", JsonUtils.toJson(sublayoutsMsg),
				service.getToken());

		log.info("Loading V1 " + GennySettings.mainrealm + " Realm Sublayouts");
		sublayoutsMsg = service.fetchSubLayoutsFromDb(GennySettings.mainrealm, GennySettings.mainrealm + "/sublayouts",
				"master");
		log.info("Loaded " + sublayoutsMsg.getItems().length + " V1 " + GennySettings.mainrealm + " Realm Sublayouts");
		VertxUtils.writeCachedJson(GennySettings.mainrealm, GennySettings.mainrealm.toUpperCase() + "-V1-LAYOUTS",
				JsonUtils.toJson(sublayoutsMsg), service.getToken());

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

		log.info("Loaded " + v2layouts.size() + " V2 " + GennySettings.mainrealm + "-new Realm Sublayouts");

		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(v2layouts.toArray(new BaseEntity[0]));
		msg.setParentCode("GRP_LAYOUTS");
		msg.setLinkCode("LNK_CORE");
		VertxUtils.writeCachedJson(GennySettings.mainrealm, "V2-LAYOUTS", JsonUtils.toJson(msg), service.getToken());

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

		// Test cache
		final String projectCode = "PRJ_" + GennySettings.mainrealm.toUpperCase();
		String sqlCode = "SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea where be.code='"
				+ projectCode + "'";
		log.info("sql code = " + sqlCode);
		final BaseEntity projectBe = (BaseEntity) em.createQuery(sqlCode).getSingleResult();
		log.info("DB project = [" + projectBe + "]");
		//
		service.writeToDDT(projectBe);
		final String key = projectBe.getCode();
		// final String prjJsonString =
		// VertxUtils.readCachedJson(projectBe.getRealm(),key,service.getToken()).getString("value");
		// ;
		// service.readFromDTT(key);
		// log.info("json from cache=["+prjJsonString+"]");
		// BaseEntity cachedProject =
		// JsonUtils.fromJson(prjJsonString,BaseEntity.class);
		// log.info("Cached Project = ["+cachedProject+"]");

	}

	
	// The following function is also in the serviceEndpoint. It is here because hibernate is not letting me save easily
	public void saveLayouts(final List<BaseEntity> layouts) {

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
				newLayout = service.findBaseEntityByCode(layout.getCode());
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
			newLayout.setRealm(securityService.getRealm());
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
	
	@Transactional
	private void saveProjectBes(Map<String,Map> projects, Service service) {
		log.info("Updating Project BaseEntitys ");
		
		for (String realmCode : projects.keySet()) {
			service.setCurrentRealm(realmCode);
			Map<String,Object> project = projects.get(realmCode);
			log.info("Project: "+projects.get(realmCode));

			if ("FALSE".equals((String)project.get("disable"))) {
				String realm = realmCode;
				String keycloakUrl = (String)project.get("keycloakUrl");
				String name = (String)project.get("name");
				String sheetID = (String)project.get("sheetID");
				String urlList = (String)project.get("urlList");
				String code = (String)project.get("code");
				String disable = (String)project.get("disable");
				String secret = (String)project.get("clientSecret");
				String key = (String)project.get("ENV_SECURITY_KEY");
				String encryptedPassword = (String)project.get("ENV_SERVICE_PASSWORD");
				String realmToken = serviceTokens.getServiceToken(realm);
		
				String projectCode = "PRJ_"+realm.toUpperCase();
				BaseEntity projectBe = null;
				try {
				projectBe = service.findBaseEntityByCode(projectCode);
				} catch (NoResultException e) {
					projectBe = null;
				}
				if (projectBe == null) {
					projectBe = new BaseEntity(projectCode,name);
					service.insert(projectBe);
				}
				
				
				
				projectBe = createAnswer(projectBe,"PRI_NAME",name,false);
				projectBe = createAnswer(projectBe,"PRI_CODE",projectCode,false);
				projectBe = createAnswer(projectBe,"ENV_SECURITY_KEY",key,true);
				projectBe = createAnswer(projectBe,"ENV_SERVICE_PASSWORD",encryptedPassword,true);
				projectBe = createAnswer(projectBe,"ENV_SERVICE_TOKEN",realmToken,true);
				projectBe = createAnswer(projectBe,"ENV_SECRET",secret,true);
				projectBe = createAnswer(projectBe,"ENV_SHEET_ID",sheetID,true);
				projectBe = createAnswer(projectBe,"ENV_URL_LIST",urlList,true);
				projectBe = createAnswer(projectBe,"ENV_DISABLE",disable,true);
				projectBe = createAnswer(projectBe,"ENV_REALM",realm,true);
				projectBe = createAnswer(projectBe,"ENV_KEYCLOAK_URL",keycloakUrl,true);
				
				BatchLoading bl = new BatchLoading(project,service);
                String keycloakJson = bl.constructKeycloakJson(project);
                projectBe = createAnswer(projectBe,"ENV_KEYCLOAK_JSON",keycloakJson,true);
				
				service.upsert(projectBe);
			}
		}
		
	}
	
	private BaseEntity createAnswer(BaseEntity be, final String attributeCode, final String answerValue, final Boolean privacy)
	{
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
		answer = new Answer(be,be,attribute,answerValue);
		answer.setChangeEvent(false);
		be.addAnswer(answer);
		EntityAttribute ea = be.findEntityAttribute(attribute);
		ea.setPrivacyFlag(privacy);
		ea.setRealm(be.getRealm());
		} catch (Exception e) {
			log.error("CANNOT UPDATE PROJECT "+be.getCode()+" "+e.getLocalizedMessage());
		}
		return be;

	}
	
	private void pushProjectsUrlsToDTT()
	{
		
		// fetch all projects from db
		String sqlCode = "SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea where be.code LIKE 'PRJ_%'";
		final List<BaseEntity> projectBes = (List<BaseEntity>) em.createQuery(sqlCode).getResultList();

		
		for (BaseEntity projectBe : projectBes) {
			if ("PRJ_GENNY".equals(projectBe.getCode())) {
				continue;
			}
				// push the project to the urls as keys too
				service.setCurrentRealm(projectBe.getRealm());

			//	BaseEntity be = service.findBaseEntityByCode(projectBe.getCode(),true);
				Session session = em.unwrap(org.hibernate.Session.class);
				Criteria criteria = session.createCriteria(BaseEntity.class);
				BaseEntity be = (BaseEntity)criteria
						.add(Restrictions.eq("code", projectBe.getCode()))
						.add(Restrictions.eq("realm", projectBe.getRealm()))
				                             .uniqueResult();
				String disabled = be.getValue("ENV_DISABLE","TRUE");
				if ("FALSE".equals(disabled)) {

				String urlList = be.getValue("ENV_URL_LIST","alyson.genny.life");
				String token = be.getValue("ENV_SERVICE_TOKEN","DUMMY");
				log.info(be.getRealm()+":"+be.getCode()+":token="+token);
				String[] urls = urlList.split(",");
				for (String url : urls) {
				//	try {
				//	URL aUrl = new URL(url);
					String keyUrl = url; //aUrl.getHost();
					VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, keyUrl.toUpperCase(), JsonUtils.toJson(be));
					VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "TOKEN"+keyUrl.toUpperCase(), token);
//					} catch (MalformedURLException e) {
//						log.error("Bad url for project "+projectBe.getRealm()+":"+url);
//					}
				}
			}
		}

	}
}
