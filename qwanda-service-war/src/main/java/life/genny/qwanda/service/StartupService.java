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

import java.io.File;
import java.util.Map;

/**
 * This Service bean demonstrate various JPA manipulations of {@link BaseEntity}
 *
 * @author Adam Crow
 */
@Singleton
@Startup
@Transactional

@TransactionTimeout(value = 3000, unit = TimeUnit.SECONDS)
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

	WildflyCache cacheInterface;

	@PersistenceContext
	private EntityManager em;

	@PostConstruct
//	@Transactional
	public void init() {

		cacheInterface = new WildflyCache(inDb);

		VertxUtils.init(eventBus, cacheInterface);

		securityService.setImportMode(true); // ugly way of getting past security

		if (!GennySettings.skipGoogleDocInStartup) {
			log.info("Starting Transaction for loading");

			String secret = System.getenv("GOOGLE_CLIENT_SECRET");
			String hostingSheetId = System.getenv("GOOGLE_HOSTING_SHEET_ID");
			File credentialPath = new File(System.getProperty("user.home"),
					".genny/sheets.googleapis.com-java-quickstart");

			Map<String,Map> projects = ProjectsLoading.loadIntoMap(hostingSheetId, secret, credentialPath);

			for (String projectCode : projects.keySet()) {
				log.info("Project: "+projects.get(projectCode));
				Map<String,Map> project = projects.get(projectCode);
				if ("FALSE".equals(project.get("disable"))) {
					log.info("PROJECT "+project.get("code"));
					// save urls to Keycloak maps
					
					BatchLoading bl = new BatchLoading(project,service);
					bl.persistProject(false, null, false);
				
	                String keycloakJson = bl.constructKeycloakJson();
	                bl.upsertKeycloakJson(keycloakJson);
	                bl.upsertProjectUrls(project.get("urlList"));
				}
			}
			
			// now load all keycloak jsons into the Keycloak Path Map
			SearchEntity searchProjectsBE = new SearchEntity("SER_PROJECTS", "Projects");
			try {
				searchBE.setValue(new AttributeInteger("SCH_PAGE_START", "PageStart"), 0);
				searchBE.setValue(new AttributeInteger("SCH_PAGE_SIZE", "PageSize"), 1000);
				searchBE.addFilter("PRI_CODE", SearchEntity.StringFilter.LIKE, "PRJ_%");

			} catch (BadDataException e) {
				log.error("Bad Data Exception");
			}

			List<BaseEntity> projectBEs = service.findBySearchBE(searchProjectsBE);

			for (BaseEntity projectBE : projectBEs) {
				String keycloakJson = projectBE.getValue("ENV_KEYCLOAK_JSON");
				String realm = projectBE.getCode().subString(4).toLowerCase();
				String urlList = projectBE.getValue("ENV_URL_LIST");
				String[] urls = urlList.split(",");
				SecureResources.getKeycloakJsonMap().put(realm,keycloakJson);
				for (String url : urls) {
					SecureResources.getKeycloakJsonMap().put(url,keycloakJson);
				}
			}
			
////			 for (List<Map<String, Object>> project : finalProjects) {
//				 bl.persistProject(project,false, null, false);
//			 }
			log.info("*********************** Finished Google Doc Import ***********************************");
		} else {
			log.info("Skipping Google doc loading");
		}

		// Push BEs to cache
		if (GennySettings.loadDdtInStartup) {
			pushToDTT();
		}
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
}
