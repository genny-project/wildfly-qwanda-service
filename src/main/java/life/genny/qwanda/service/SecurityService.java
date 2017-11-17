package life.genny.qwanda.service;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;

import life.genny.qwanda.CoreEntity;
import life.genny.qwandautils.KeycloakUtils;

/**
 * Transactional Security Service
 *
 * @author Adam Crow
 */

@Stateless

public class SecurityService implements Serializable {
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Context
	SecurityContext sc;

	@Inject
	private Principal principal;

	KeycloakSecurityContext kc = null;

	Map<String, Object> user = new HashMap<String, Object>();

	static boolean importMode = false;

	@PostConstruct
	public void init() {
		if (!importMode) {
			log.info("hello " + principal);
			kc = getKeycloakUser();
			if (kc != null) {
				user = KeycloakUtils.getJsonMap(kc.getTokenString());
				log.info("User:" + user);
			} else {
				log.error("cannot init kc (keycloak user)");
			}
		} else {
			log.info("Import Mode Security off");
		}
	}

	public String getRealm() {
		if (!importMode) {
			return kc.getRealm();
		} else {
			return CoreEntity.DEFAULT_REALM;
		}
	}

	public Map<String, Object> getUserMap() {
		return user;
	}

	public boolean isAuthorised() {

		return true;
	}

	private KeycloakSecurityContext getKeycloakUser() {
		if (sc != null) {
			if (sc.getUserPrincipal() != null) {
				if (sc.getUserPrincipal() instanceof KeycloakPrincipal) {
					final KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) sc
							.getUserPrincipal();

					return kp.getKeycloakSecurityContext();
				}
			}
		}
		// throw new SecurityException("Unauthorised User");
		return null;
	}

	public static void setImportMode(final boolean mode) {
		importMode = mode;
	}

	public String getToken() {
		if (kc == null) {
			log.error("No keycloak context in SecurityService");
			return "NO KEYCLOAK CONTEXT";
		}
		return kc.getTokenString();
	}

}
