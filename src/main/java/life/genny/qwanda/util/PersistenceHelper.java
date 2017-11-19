package life.genny.qwanda.util;

import java.lang.invoke.MethodHandles;

import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

import org.apache.logging.log4j.Logger;

@ApplicationScoped
@Singleton
public class PersistenceHelper {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	// @PersistenceContext(unitName = "genny-persistence-unit")
	@PersistenceContext(unitName = "genny-persistence-unit", type = PersistenceContextType.EXTENDED)
	private EntityManager em;

	public EntityManager getEntityManager() {
		if (em == null) {
			log.error("PersistenceHelper entityManager is null!!!");
		}
		return em;
	}
}
