package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;
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
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwandautils.JsonUtils;
import life.genny.services.BatchLoading;

import life.genny.eventbus.EventBusInterface;
import io.vertx.resourceadapter.examples.mdb.EventBusBean;
import io.vertx.resourceadapter.examples.mdb.WildflyCache;
import javax.inject.Inject;
import life.genny.utils.VertxUtils;
import life.genny.qwandautils.GennySettings;

import life.genny.qwanda.message.QDataSubLayoutMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;



import life.genny.qwandautils.GitUtils;

/**
 * This Service bean demonstrate various JPA manipulations of {@link BaseEntity}
 *
 * @author Adam Crow
 */
@Singleton
@Startup
@Transactional

@TransactionTimeout(value=3000, unit=TimeUnit.SECONDS)
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
		
		VertxUtils.init(eventBus,cacheInterface);

		securityService.setImportMode(true); // ugly way of getting past security

		// em = emf.createEntityManager();
		if ((System.getenv("SKIP_GOOGLE_DOC_IN_STARTUP")==null)||(!System.getenv("SKIP_GOOGLE_DOC_IN_STARTUP").equalsIgnoreCase("TRUE"))) {
			log.info("Starting Transaction for loading");
			BatchLoading bl = new BatchLoading(service);
			bl.persistProject(false, null, false);
			log.info("*********************** Finished Google Doc Import ***********************************");
		} else {
			log.info("Skipping Google doc loading");
		}

		// Push BEs to cache
		if (System.getenv("LOAD_DDT_IN_STARTUP")!=null) {
			pushToDTT();
		}

		String accessToken = service.getServiceToken(GennySettings.mainrealm);
		log.info("ACCESS_TOKEN: " + accessToken);
		service.sendQEventSystemMessage("EVT_QWANDA_SERVICE_STARTED", accessToken);
		
		log.info("Loading V1 Genny Sublayouts");
		QDataSubLayoutMessage sublayoutsMsg = service.fetchSubLayoutsFromDb(GennySettings.mainrealm, "genny/sublayouts", "master");
		VertxUtils.writeCachedJson(GennySettings.mainrealm,"GENNY-V1-LAYOUTS",JsonUtils.toJson(sublayoutsMsg), service.getToken());

		log.info("Loading V1 Realm Sublayouts");
		sublayoutsMsg = service.fetchSubLayoutsFromDb(GennySettings.mainrealm, GennySettings.mainrealm+"/sublayouts", "master");
		VertxUtils.writeCachedJson(GennySettings.mainrealm,GennySettings.mainrealm.toUpperCase()+"-V1-LAYOUTS",JsonUtils.toJson(sublayoutsMsg), service.getToken());

//		List<BaseEntity> v2layouts = service.fetchSubLayoutsFromDb(GennySettings.mainrealm, GennySettings.mainrealm+"-new", "master");
//		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(v2layouts.toArray(new BaseEntity[0]));
//		msg.setParentCode("GRP_LAYOUTS");
//		msg.setLinkCode("LNK_CORE");
//		VertxUtils.writeCachedJson(GennySettings.mainrealm, "V2-LAYOUTS", JsonUtils.toJson(msg),
//				service.getToken());
//		
		
		
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
		log.info("Pushed "+entitys.size()+" attributes to cache");
		
		// BaseEntitys
		List<BaseEntity> results = em
				.createQuery("SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea ").getResultList();

//		List<BaseEntity> results = em
//				.createQuery("SELECT be FROM BaseEntity be  JOIN  be.baseEntityAttributes ea ").getResultList();

		
		// Collect all the baseentitys
		log.info("Pushing "+results.size()+" Basentitys to Cache");
		service.writeToDDT(results);
		log.info("Pushed "+results.size()+" Basentitys to Cache");		
	

		// Test cache
		final String projectCode = "PRJ_"+GennySettings.mainrealm.toUpperCase();
		String sqlCode = "SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea where be.code='"+projectCode+"'";
		log.info("sql code = "+sqlCode);
		final BaseEntity projectBe = (BaseEntity)em
				.createQuery(sqlCode).getSingleResult();
		log.info("DB project = ["+projectBe+"]");
		//
		service.writeToDDT(projectBe);
		final String key = projectBe.getCode();
	//	final String prjJsonString = VertxUtils.readCachedJson(projectBe.getRealm(),key,service.getToken()).getString("value"); ;
		//service.readFromDTT(key);
	//	log.info("json from cache=["+prjJsonString+"]");
	//	BaseEntity cachedProject = JsonUtils.fromJson(prjJsonString,BaseEntity.class);
	//	log.info("Cached Project = ["+cachedProject+"]");
						
		

	}

}
