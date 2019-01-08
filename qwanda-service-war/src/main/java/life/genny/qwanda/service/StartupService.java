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

/**
 * This Service bean demonstrate various JPA manipulations of {@link BaseEntity}
 *
 * @author Adam Crow
 */
@Singleton
@Startup
@Transactional
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
	@Transactional
	public void init() {

		cacheInterface = new WildflyCache(inDb);
		
		VertxUtils.init(eventBus,cacheInterface);

		securityService.setImportMode(true); // ugly way of getting past security

		// em = emf.createEntityManager();
		if ((System.getenv("SKIP_GOOGLE_DOC_IN_STARTUP")==null)||(!System.getenv("SKIP_GOOGLE_DOC_IN_STARTUP").equalsIgnoreCase("TRUE"))) {
			System.out.println("Starting Transaction for loading");
			BatchLoading bl = new BatchLoading(service);
			bl.persistProject(false, null, false);
			System.out.println("*********************** Finished Google Doc Import ***********************************");
		} else {
			System.out.println("Skipping Google doc loading");
		}
		securityService.setImportMode(false);

		// Push BEs to cache
		if (System.getenv("LOAD_DDT_IN_STARTUP")!=null) {
			pushToDTT();
		}

		service.sendQEventSystemMessage("EVT_QWANDA_SERVICE_STARTED", service.getServiceToken(GennySettings.mainrealm));
		// em.close();
		// emf.close();
	}

	// @javax.ejb.Asynchronous
	public void pushToDTT() {
		// BaseEntitys
		List<BaseEntity> results = em
				.createQuery("SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea ").getResultList();
	
		
		// Collect all the baseentitys
		System.out.println("Pushing "+results.size()+" Basentitys to Cache");
		service.writeToDDT(results);
		
	
		// Attributes
		System.out.println("Pushing Attributes to Cache");
		final List<Attribute> entitys = service.findAttributes();
		Attribute[] atArr = new Attribute[entitys.size()];
		atArr = entitys.toArray(atArr);
		QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
		String json = JsonUtils.toJson(msg);
		service.writeToDDT("attributes", json);
		System.out.println("---------------- Completed Startup ----------------");
	}

}
