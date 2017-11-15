package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.util.PersistenceHelper;
import life.genny.services.BatchLoading;

/**
 * This Service bean demonstrate various JPA manipulations of {@link BaseEntity}
 *
 * @author Adam Crow
 */
@Singleton
@Startup
public class StartupService {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	// @Inject
	// private BaseEntityService service;

	@Inject
	private SecurityService securityService;

	@Inject
	private PersistenceHelper helper;

	@PostConstruct
	public void init() {
		securityService.setImportMode(true); // ugly way of getting past security

		BatchLoading bl = new BatchLoading(helper.getEntityManager());
		bl.persistProject();
		System.out.println("***********************&&&&&&*8778878877877006oy***********************************");
		securityService.setImportMode(false);
	}

}
