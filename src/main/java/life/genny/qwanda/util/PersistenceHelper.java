package life.genny.qwanda.util;

import javax.enterprise.context.RequestScoped;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceContextType;

@RequestScoped
public class PersistenceHelper {

	@PersistenceContext(unitName = "genny-persistence-unit", type = PersistenceContextType.EXTENDED)
	private EntityManager em;

	public EntityManager getEntityManager() {
		return em;
	}
}
