package life.genny.qwanda.util;

import java.lang.invoke.MethodHandles;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.apache.logging.log4j.Logger;

//@ApplicationScoped
//@Singleton
@RequestScoped
public class PersistenceHelper {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@PersistenceContext(unitName = "genny-persistence-unit")
	// @PersistenceContext(unitName = "genny-persistence-unit", type =
	// PersistenceContextType.EXTENDED)
	private EntityManager em;

	public EntityManager getEntityManager() {
		if (em == null) {
			log.error("PersistenceHelper entityManager is null!!!");
		}
		return em;
	}
}
