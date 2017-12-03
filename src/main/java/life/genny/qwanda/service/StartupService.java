package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;

import org.apache.logging.log4j.Logger;

import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.service.SecurityService;
import life.genny.services.BaseEntityService2;
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
	private BaseEntityService2 service;

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
		System.out.println("***********************&&&&&&*8778878877877006oy***********************************");
		securityService.setImportMode(false);
		// em.close();
		// emf.close();
	}

}
