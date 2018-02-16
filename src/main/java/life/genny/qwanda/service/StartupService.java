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

import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwandautils.JsonUtils;
import life.genny.services.BatchLoading;

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
	private Service service;

	@Inject
	private SecurityService securityService;

	// @PersistenceContext(unitName = "genny-persistence-unit", type =
	// PersistenceContextType.EXTENDED)
	@PersistenceContext
	private EntityManager em;

	// @PersistenceUnit(unitName = "genny-persistence-unit")
	// EntityManagerFactory emf;

	// @Inject
	// private PersistenceHelper helper;

	@PostConstruct
	public void init() {
		securityService.setImportMode(true); // ugly way of getting past security

		// em = emf.createEntityManager();
		System.out.println("Starting Transaction for loading");
		BatchLoading bl = new BatchLoading(service);
		bl.persistProject();
		System.out.println("*********************** Finished Google Doc Import ***********************************");
		securityService.setImportMode(false);

		// Push BEs to cache
		// pushToDTT();

		service.sendQEventSystemMessage("EVT_QWANDA_SERVICE_STARTED", "NO_TOKEN");
		// em.close();
		// emf.close();
	}

	// @javax.ejb.Asynchronous
	public void pushToDTT() {
		List<BaseEntity> results = em
				.createQuery("SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea ").getResultList();
		for (BaseEntity be : results) {
			String json = JsonUtils.toJson(be);
			service.writeToDDT(be.getCode(), json);
		}
	}

}
