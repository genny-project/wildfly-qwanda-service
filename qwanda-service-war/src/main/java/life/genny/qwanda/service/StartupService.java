package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;
import org.apache.logging.log4j.Logger;
import org.hibernate.Session;
import org.jboss.ejb3.annotation.TransactionTimeout;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import io.vertx.resourceadapter.examples.mdb.EventBusBean;
import io.vertx.resourceadapter.examples.mdb.WildflyCache;
import life.genny.bootxport.bootx.GoogleImportService;
import life.genny.bootxport.bootx.Realm;
import life.genny.bootxport.bootx.RealmUnit;
import life.genny.bootxport.bootx.XSSFService;
//import life.genny.bootxport.bootx.RealmUnit;
import life.genny.bootxport.bootx.XlsxImport;
import life.genny.bootxport.bootx.XlsxImportOffline;
import life.genny.bootxport.bootx.XlsxImportOnline;
//import life.genny.services.BatchLoading;
import life.genny.bootxport.xlsimport.BatchLoading;
import life.genny.qwanda.Answer;
import life.genny.qwanda.Layout;
import life.genny.qwanda.Question;
import life.genny.qwanda.QuestionQuestion;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.datatype.DataType;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.security.SecureResources;
import life.genny.utils.VertxUtils;




/**
 * This Service bean Starts up the main API service. Loading in the
 * database/google bootstrap/github layouts
 *
 * @author Adam Crow
 */
@Singleton
@Startup
@Transactional

@TransactionTimeout(value = 80000, unit = TimeUnit.SECONDS)
public class StartupService {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public void persistEnabledProject(RealmUnit realmUnit) {
		if (!realmUnit.getDisable()) {
			log.info("Project: " + realmUnit.getCode());

			Boolean skipGoogleDoc = realmUnit.getSkipGoogleDoc();

			if ((skipGoogleDoc != null)
					&& !realmUnit.getDisable()) {
				String realm = realmUnit.getCode();
				service.setCurrentRealm(realm);
				log.info("PROJECT " + realm);
				BatchLoading bl = new BatchLoading(service);

				// save urls to Keycloak maps
				service.setCurrentRealm(realmUnit.getCode()); // provide overridden realm

                bl.persistProject(realmUnit);
				String keycloakJson = bl.constructKeycloakJson(realmUnit);
				bl.upsertKeycloakJson(keycloakJson);
				bl.upsertProjectUrls((String) realmUnit.getUrlList());
			}
		}
	}

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

//	@PostConstruct
//	@Transactional
//	public void init() {
//
//        GoogleImportService gs = GoogleImportService.getInstance();
//        XlsxImport xlsImport = new XlsxImportOnline(gs.getService());
//        Realm rx = new Realm(xlsImport, System.getenv("GOOGLE_HOSTING_SHEET_ID"));
//        
//		cacheInterface = new WildflyCache(inDb);
//
//		VertxUtils.init(eventBus, cacheInterface);
//
//		securityService.setImportMode(true); // ugly way of getting past security
//
////		String secret = System.getenv("GOOGLE_CLIENT_SECRET");
////		String hostingSheetId = System.getenv("GOOGLE_HOSTING_SHEET_ID");
////		File credentialPath = new File(System.getProperty("user.home"), ".genny/sheets.googleapis.com-java-quickstart");
//
////		Map<String, Map> projects = ProjectsLoading.loadIntoMap(hostingSheetId, secret, credentialPath);
//
////		serviceTokens.init(projects);
//		rx.getDataUnits().forEach(serviceTokens::init);
//
//
//		// Save projects
//		rx.getDataUnits().forEach(this::saveProjectBes);
//		rx.getDataUnits().forEach(this::saveServiceBes);
//		rx.getDataUnits().forEach(this::pushProjectsUrlsToDTT);
//
//		if (!GennySettings.skipGoogleDocInStartup) {
//			log.info("Starting Transaction for loading");
//
//			rx.getDataUnits().forEach(project -> {
////			for (String projectCode : projects.keySet()) {
//			  
//			
//				if ("FALSE".equals((String) project.getDisable().toString().toUpperCase())) {
//					log.info("Project: " + project.getCode());
//
//					String skipGoogleDoc = project.getSkipGoogleDoc().toString().toUpperCase();
//
//					if ((skipGoogleDoc != null)
//							&& ("FALSE".equals((project.getSkipGoogleDoc().toString().toUpperCase())))) {
//						String realm = project.getCode();
//						service.setCurrentRealm(realm);
//						log.info("PROJECT " + realm);
//						BatchLoading bl = new BatchLoading(service);
//
//						// save urls to Keycloak maps
//						service.setCurrentRealm(project.getCode()); // provide overridden realm
//
//						bl.persistProject(project);
//						String keycloakJson = bl.constructKeycloakJson(project);
//						bl.upsertKeycloakJson(keycloakJson);
//						// bl.upsertProjectUrls((String) project.get("urlList"));
//					}
////				}
//			  
//    			}
//			});
//			log.info("*********************** Finished Google Doc Import ***********************************");
//		} else {
//			log.info("Skipping Google doc loading");
//		}
//
//		// Push BEs to cache
//		if (System.getenv("LOAD_DDT_IN_STARTUP") != null) {
//			rx.getDataUnits().forEach(this::pushToDTT);
////			pushToDTT(projects);
//		}
//
//    	rx.getDataUnits().forEach(this::pushProjectsUrlsToDTT);
////		pushProjectsUrlsToDTT(projects);
//
//		log.info("skipGithubInStartup is " + (GennySettings.skipGithubInStartup ? "TRUE" : "FALSE"));
//
////		for (String realm : projects.keySet()) {
//		  rx.getDataUnits().forEach(project ->{
////			Map<String, Object> project = projects.get(realm);
//			if ("FALSE".equals((String) project.getDisable().toString().toUpperCase())) {
//				service.setCurrentRealm(project.getCode());
//				String accessToken = serviceTokens.getServiceToken(project.getCode());
//				service.sendQEventSystemMessage("EVT_QWANDA_SERVICE_STARTED", accessToken);
//			}
//	    
//		  });
////			Map<String, Object> project = projects.get(realm);
////			if ("FALSE".equals((String) project.get("disable"))) {
////				service.setCurrentRealm(realm);
////				String accessToken = serviceTokens.getServiceToken(realm);
////				service.sendQEventSystemMessage("EVT_QWANDA_SERVICE_STARTED", accessToken);
////			}
////		}
//
//		log.info("---------------- Completed Startup ----------------");
//		securityService.setImportMode(false);
//
//	}

	@PostConstruct
	@Transactional
	public void inits() {
	  
		cacheInterface = new WildflyCache(inDb);
		VertxUtils.init(eventBus, cacheInterface);
		securityService.setImportMode(true); // ugly way of getting past security
        GoogleImportService gs = GoogleImportService.getInstance();
    
        Boolean onlineMode = Optional.ofNullable(System.getenv("ONLINE_MODE"))
            .map(val -> val.toLowerCase())
            .map(Boolean::getBoolean)
            .orElse(true);

        XlsxImport xlsImport;
        XSSFService service = new  XSSFService();
        if(onlineMode){
            xlsImport = new XlsxImportOnline(gs.getService());
        }else{
            xlsImport = new XlsxImportOffline(service);
        }
        Realm rx = new Realm(xlsImport, 
            System.getenv("GOOGLE_HOSTING_SHEET_ID"));

        List<String> realms = rx.getDataUnits().stream()
            .filter(r-> !r.getDisable())
            .map(d -> d.getCode())
            .collect(Collectors.toList());

        rx.getDataUnits().forEach(serviceTokens::init);
		// Save projects
		rx.getDataUnits().forEach(this::saveProjectBes);
		rx.getDataUnits().forEach(this::saveServiceBes);
		rx.getDataUnits().forEach(this::pushProjectsUrlsToDTT);

		if (!GennySettings.skipGoogleDocInStartup) {
			log.info("Starting Transaction for loading");

			rx.getDataUnits().forEach(this::persistEnabledProject);
			log.info("*********************** Finished Google Doc Import ***********************************");
		} else {
			log.info("Skipping Google doc loading");
		}

		// Push BEs to cache
		if (GennySettings.loadDdtInStartup) {		    
            log.info("Pushing to DTT  ");
		    rx.getDataUnits().forEach(this::pushToDTT);
		}
		rx.getDataUnits().forEach(this::pushProjectsUrlsToDTT);
		log.info("skipGithubInStartup is " + (GennySettings.skipGithubInStartup ? "TRUE" : "FALSE"));
		String branch = "master";
		rx.getDataUnits().forEach(this::setEnabledRealm);
		log.info("---------------- Completed Startup ----------------");
		securityService.setImportMode(false);

		// Push the list of active realms
		if(!(realms.size() == 0)) {
          String realmsJson = JsonUtils.toJson(realms);
		  VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "REALMS", realmsJson);
		}

	}

//	private void pushProjectsUrlsToDTT(RealmUnit realmUnit) {
//
//		final List<String> realms = service.getRealms();
//
//		List<String> activeRealms = new ArrayList<String>(); // build up the active realms to put into a single location
//																// in the cache
//
//			if (!realmUnit.getDisable()) {
//
//				// push the project to the urls as keys too
//				service.setCurrentRealm(realmUnit.getCode());
//				activeRealms.add(realmUnit.getCode());
//
//				String realm = realmUnit.getCode();
//				BaseEntity be = null; // service.findBaseEntityByCode("PRJ_" + realm.toUpperCase(), true);
//				CriteriaBuilder cb = em.getCriteriaBuilder();
//				CriteriaQuery<BaseEntity> query = cb.createQuery(BaseEntity.class);
//				Root<BaseEntity> root = query.from(BaseEntity.class);
//
//				query = query.select(root).where(cb.equal(root.get("code"), "PRJ_" + realm.toUpperCase()),
//						cb.equal(root.get("realm"), realm));
//
//				try {
//					be = em.createQuery(query).getSingleResult();
//				} catch (NoResultException nre) {
//
//				}
////				Session session = em.unwrap(org.hibernate.Session.class);
////				Criteria criteria = session.createCriteria(BaseEntity.class);
////				BaseEntity be = (BaseEntity)criteria
////						.add(Restrictions.eq("code", projectBe.getCode()))
////						.add(Restrictions.eq("realm", projectBe.getRealm()))
////				                             .uniqueResult();
//
//				String urlList = be.getValue("ENV_URL_LIST", "alyson3.genny.life");
//				String token = serviceTokens.getServiceToken(realm); // be.getValue("ENV_SERVICE_TOKEN", "DUMMY");
//
//				// log.info(be.getRealm() + ":" + be.getCode() + ":token=" + token);
//				VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase(), token);
//				VertxUtils.putObject(realm, "CACHE", "SERVICE_TOKEN", token);
//				String[] urls = urlList.split(",");
//				for (String url : urls) {
//					URL aURL = null;
//					try {
//						if (!((url.startsWith("http:")) || (url.startsWith("https:")))) {
//							url = "http://" + url; // hack
//						}
//						aURL = new URL(url);
//						final String cleanUrl = aURL.getHost();
//						log.info("Writing to Cache: " + GennySettings.GENNY_REALM + ":" + cleanUrl.toUpperCase());
//						VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, cleanUrl.toUpperCase(),
//								JsonUtils.toJson(be));
//						VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "TOKEN" + cleanUrl.toUpperCase(), token);
//					} catch (MalformedURLException e) {
//						log.error("Bad URL for realm " + be.getRealm() + "=" + url);
//					}
//				}
//			//
//		}
//		// Push the list of active realms
//		Type listType = new TypeToken<List<String>>() {
//		}.getType();
//		Gson gson = new Gson();
//		String realmsJson = JsonUtils.toJson(activeRealms);
//		VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "REALMS", realmsJson);
//	}

//	private void saveProjectBes(RealmUnit realmUnit) {
//		log.info("Updating Project BaseEntitys ");
//
//		String realmCode = realmUnit.getCode();
//
//		if (!realmUnit.getDisable()) {
//
//			service.setCurrentRealm(realmCode);
//			log.info("Project: " + realmCode);
//
//				String realm = realmCode;
//				String keycloakUrl = (String) realmUnit.getKeycloakUrl();
//				String name = (String) realmUnit.getName();
//				String urlList = realmUnit.getUrlList();
//				String code = realmUnit.getCode();
//				Boolean disable = realmUnit.getDisable();
//				String secret = realmUnit.getClientSecret(); 
//				String key = realmUnit.getSecurityKey();
//				String encryptedPassword = realmUnit.getServicePassword();
//				String realmToken = serviceTokens.getServiceToken(realm);
//				Boolean skipGoogleDoc = realmUnit.getSkipGoogleDoc();
//				String projectCode = "PRJ_" + realm.toUpperCase();
//				BaseEntity projectBe = null;
//
//				projectBe = new BaseEntity(projectCode, name);
//				projectBe = service.upsert(projectBe);
//
//				projectBe = createAnswer(projectBe, "PRI_NAME", name, false);
//				projectBe = createAnswer(projectBe, "PRI_CODE", projectCode, false);
//				projectBe = createAnswer(projectBe, "ENV_SECURITY_KEY", key, true);
//				projectBe = createAnswer(projectBe, "ENV_SERVICE_PASSWORD", encryptedPassword, true);
//				projectBe = createAnswer(projectBe, "ENV_SERVICE_TOKEN", realmToken, true);
//				projectBe = createAnswer(projectBe, "ENV_SECRET", secret, true);
////				projectBe = createAnswer(projectBe, "ENV_SHEET_ID", sheetID, true);
//				projectBe = createAnswer(projectBe, "ENV_URL_LIST", urlList, true);
//				projectBe = createAnswer(projectBe, "ENV_DISABLE", disable.toString().toUpperCase(), true);
//				projectBe = createAnswer(projectBe, "ENV_REALM", realm, true);
//				projectBe = createAnswer(projectBe, "ENV_KEYCLOAK_URL", keycloakUrl, true);
//				projectBe = createAnswer(projectBe, "ENV_KEYCLOAK_REDIRECTURI", keycloakUrl, true);
//
//				BatchLoading bl =  new BatchLoading(service);
//				String keycloakJson = bl.constructKeycloakJson(realmUnit);
//
//				projectBe = createAnswer(projectBe, "ENV_KEYCLOAK_JSON", keycloakJson, true);
//
//				projectBe = service.upsert(projectBe);
//
//				// Set up temp keycloak.json Maps
//				String[] urls = urlList.split(",");
//				SecureResources.addRealm(realm, keycloakJson);
//				SecureResources.addRealm(realm + ".json", keycloakJson);
//				// redundant
//				if (("genny".equals(realm))) {
//					SecureResources.addRealm("genny", keycloakJson);
//					SecureResources.addRealm("genny.json", keycloakJson);
//					SecureResources.addRealm("qwanda-service.genny.life.json", keycloakJson);
//				}
//
//				// Overwrite all the time, must have localhost
//				SecureResources.addRealm("localhost.json", keycloakJson);
//				SecureResources.addRealm("localhost", keycloakJson);
//				SecureResources.addRealm("localhost:8080", keycloakJson);
//				for (String url : urls) {
//					SecureResources.addRealm(url + ".json", keycloakJson);
//					SecureResources.addRealm(url, keycloakJson);
//				}
//
//				// Save project BE in a consistent place
//				VertxUtils.putObject(realm, "", "PROJECT", JsonUtils.toJson(projectBe),
//						serviceTokens.getServiceToken(realm));
//
//			}
//
//	}

//	private void saveServiceBes(RealmUnit realmUnit) {
//		log.info("Updating Service BaseEntitys ");
//
//			if (!realmUnit.getDisable()) {
//
//			  String realmCode = realmUnit.getCode();
//				service.setCurrentRealm(realmCode);
//				log.info("Service: " + realmCode);
//
//					String realm = realmCode;
//					String name = realmUnit.getName() + " Service User";
//					String realmToken = serviceTokens.getServiceToken(realm);
//
//					String serviceCode = "PER_SERVICE";
//					BaseEntity serviceBe = null;
//
//					serviceBe = new BaseEntity(serviceCode, name);
//					serviceBe = service.upsert(serviceBe);
//
//					serviceBe = createAnswer(serviceBe, "PRI_NAME", name, false);
//					serviceBe = createAnswer(serviceBe, "PRI_CODE", serviceCode, false);
//					serviceBe = createAnswer(serviceBe, "ENV_SERVICE_TOKEN", realmToken, true);
//
//					serviceBe = service.upsert(serviceBe);
//
//			}
//
//	}

   	public void setEnabledRealm(RealmUnit realmUnit) {
		if (!realmUnit.getDisable()) {
		    String realm = realmUnit.getCode();
			service.setCurrentRealm(realm);
			String accessToken = serviceTokens.getServiceToken(realm);
			service.sendQEventSystemMessage("EVT_QWANDA_SERVICE_STARTED", accessToken);
		}
	}


//	public void pushToDTT(RealmUnit realmUnit) {
//		// Attributes
//		log.info("Pushing Attributes to Cache");
//		final List<Attribute> entitys = service.findAttributes();
//		Attribute[] atArr = new Attribute[entitys.size()];
//		atArr = entitys.toArray(atArr);
//		QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
//		String json = JsonUtils.toJson(msg);
//		service.writeToDDT("attributes", json);
//		log.info("Pushed " + entitys.size() + " attributes to cache");
//
//		// BaseEntitys
////		List<BaseEntity> results = em
////				.createQuery("SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea ").getResultList();
//		Session session = em.unwrap(org.hibernate.Session.class);
//
//			if (!realmUnit.getDisable()) {
//
//				service.setCurrentRealm(realmUnit.getCode());
//				log.info("Project: " + realmUnit.getCode() + " push to DDT");
//
//				String realm = realmUnit.getCode();
//
//				CriteriaBuilder builder = em.getCriteriaBuilder();
//				pushBEsToCache(builder, realm);
//				pushQuestionsToCache(builder, realm);
//			}
//
//
//		// Collect all the baseentitys
//
//	}


	public void pushToDTT(RealmUnit realmUnit) {
		// Attributes
		log.info("Pushing Attributes to Cache");
		final List<Attribute> entitys = service.findAttributes();
		Attribute[] atArr = new Attribute[entitys.size()];
		atArr = entitys.toArray(atArr);
		QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
		String json = JsonUtils.toJson(msg);
		service.writeToDDT("attributes", json);
		log.info("Pushed " + entitys.size() + " attributes to cache");

		String realmCode = realmUnit.getCode();
		// BaseEntitys
//		List<BaseEntity> results = em
//				.createQuery("SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea ").getResultList();
		Session session = em.unwrap(org.hibernate.Session.class);

//		for (String realmCode : projects.keySet()) {
//			Map<String, Object> project = projects.get(realmCode);
			if ("FALSE".equals((String) realmUnit.getDisable().toString().toUpperCase())) {

				service.setCurrentRealm(realmCode);
				log.info("Project: " + realmCode + " push to DDT");

				String realm = realmCode;

				CriteriaBuilder builder = em.getCriteriaBuilder();
				pushBEsToCache(builder, realm);
				pushQuestionsToCache(builder, realm);
			}

//		}

		// Collect all the baseentitys

	}

	private void pushBEsToCache(CriteriaBuilder builder, final String realm) {
		CriteriaQuery<BaseEntity> query = builder.createQuery(BaseEntity.class);
		Root<BaseEntity> be = query.from(BaseEntity.class);
		Join<BaseEntity, EntityAttribute> ea = (Join) be.fetch("baseEntityAttributes");
		query.select(be);
		query.distinct(true);
		query.where(builder.equal(ea.get("realm"), realm));

		List<BaseEntity> results = em.createQuery(query).getResultList();

		log.info("Pushing " + realm + " : " + results.size() + " Basentitys to Cache");
		service.writeToDDT(results);
		log.info("Pushed " + realm + " : " + results.size() + " Basentitys to Cache");

	}

	private void pushQuestionsToCache(CriteriaBuilder builder, final String realm) {
		CriteriaQuery<Question> query = builder.createQuery(Question.class);
		Root<Question> be = query.from(Question.class);
		Join<Question, QuestionQuestion> ea = (Join) be.fetch("childQuestions");
		query.select(be);
		query.distinct(true);
		query.where(builder.equal(ea.get("realm"), realm));

		List<Question> results = em.createQuery(query).getResultList();

		log.info("Pushing " + realm + " : " + results.size() + " Questions to Cache");
		service.writeQuestionsToDDT(results);
		log.info("Pushed " + realm + " : " + results.size() + " Questions to Cache");

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

	private void saveProjectBes(RealmUnit realmUnit) {
		log.info("Updating Project BaseEntitys ");

//		for (String realmCode : projects.keySet()) {
//			Map<String, Object> project = projects.get(realmCode);
			if ("FALSE".equals((String) realmUnit.getDisable().toString().toUpperCase())) {

			    String realmCode = realmUnit.getCode();
				service.setCurrentRealm(realmCode);
				log.info("Project: " + realmCode);

				if ("FALSE".equals((String)realmUnit.getDisable().toString().toUpperCase())) {
					String realm = realmCode;
					String keycloakUrl = (String) realmUnit.getKeycloakUrl();
					String name = (String) realmUnit.getName() ;
					String sheetID = (String) realmUnit.getUri();
					String urlList = (String) realmUnit.getUrlList();
					String code = (String) realmUnit.getCode();
					String disable = (String) realmUnit.getDisable().toString().toUpperCase();
					String secret = (String)  realmUnit.getClientSecret();
					String key = (String) realmUnit.getSecurityKey();
					String encryptedPassword = realmUnit.getServicePassword();
					String realmToken = serviceTokens.getServiceToken(realm);
					String skipGoogleDoc = (String) realmUnit.getSkipGoogleDoc().toString().toUpperCase();
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
					projectBe = createAnswer(projectBe, "ENV_KEYCLOAK_REDIRECTURI", keycloakUrl, true);
					BatchLoading bl = new BatchLoading(service);
					String keycloakJson = bl.constructKeycloakJson(realmUnit);
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

					// Save project BE in a consistent place
					VertxUtils.putObject(realm, "", "PROJECT", JsonUtils.toJson(projectBe),
							serviceTokens.getServiceToken(realm));

				}
			}
//		}

	}

	private void saveServiceBes(RealmUnit realmUnit) {
		log.info("Updating Service BaseEntitys ");

//		for (String realmCode : projects.keySet()) {
//			Map<String, Object> project = projects.get(realmCode);
			if ("FALSE".equals((String) realmUnit.getDisable().toString().toUpperCase())) {

			    String realmCode = realmUnit.getCode();
				service.setCurrentRealm(realmCode);
				log.info("Service: " + realmCode);

				if ("FALSE".equals((String) realmUnit.getDisable().toString().toUpperCase())) {
					String realm = realmCode;
					String name = realmUnit.getName() + " Service User";
					String realmToken = serviceTokens.getServiceToken(realm);

					String serviceCode = "PER_SERVICE";
					BaseEntity serviceBe = null;

					serviceBe = new BaseEntity(serviceCode, name);
					serviceBe = service.upsert(serviceBe);

					serviceBe = createAnswer(serviceBe, "PRI_NAME", name, false);
					serviceBe = createAnswer(serviceBe, "PRI_CODE", serviceCode, false);
					serviceBe = createAnswer(serviceBe, "ENV_SERVICE_TOKEN", realmToken, true);

					serviceBe = service.upsert(serviceBe);

				}
			}
//		}

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

	private void pushProjectsUrlsToDTT(RealmUnit realmUnit) {

//		final List<String> realms = service.getRealms();

//		List<String> activeRealms = new ArrayList<String>(); // build up the active realms to put into a single location
																// in the cache
			    String realm = realmUnit.getCode();

//		for (String realm : realms) {
//			Map<String, Object> project = projects.get(realm);
			if ((realmUnit != null) && ("FALSE".equals((String) realmUnit.getDisable().toString().toUpperCase()))) {

				// push the project to the urls as keys too
				service.setCurrentRealm(realm);

				BaseEntity be = null; // service.findBaseEntityByCode("PRJ_" + realm.toUpperCase(), true);
				CriteriaBuilder cb = em.getCriteriaBuilder();
				CriteriaQuery<BaseEntity> query = cb.createQuery(BaseEntity.class);
				Root<BaseEntity> root = query.from(BaseEntity.class);

				query = query.select(root).where(cb.equal(root.get("code"), "PRJ_" + realm.toUpperCase()),
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
				String token = serviceTokens.getServiceToken(realm); // be.getValue("ENV_SERVICE_TOKEN", "DUMMY");

				// log.info(be.getRealm() + ":" + be.getCode() + ":token=" + token);
				VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "TOKEN" + realm.toUpperCase(), token);
				VertxUtils.putObject(realm, "CACHE", "SERVICE_TOKEN", token);
				String[] urls = urlList.split(",");
				for (String url : urls) {
					URL aURL = null;
					try {
						if (!((url.startsWith("http:")) || (url.startsWith("https:")))) {
							url = "http://" + url; // hack
						}
						aURL = new URL(url);
						final String cleanUrl = aURL.getHost();
						log.info("Writing to Cache: " + GennySettings.GENNY_REALM + ":" + cleanUrl.toUpperCase());
						VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, cleanUrl.toUpperCase(),
								JsonUtils.toJson(be));
						VertxUtils.writeCachedJson(GennySettings.GENNY_REALM, "TOKEN" + cleanUrl.toUpperCase(), token);
					} catch (MalformedURLException e) {
						log.error("Bad URL for realm " + be.getRealm() + "=" + url);
					}
				}
			}
			//
//		}
	}


	
}
