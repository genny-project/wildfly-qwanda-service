package life.genny.qwanda.service;

import java.io.Serializable;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.SecurityContext;

import org.apache.logging.log4j.Logger;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import life.genny.qwanda.CoreEntity;
import life.genny.qwandautils.QwandaUtils;

/**
 * Transactional Security Service
 *
 * @author Adam Crow
 */

@RequestScoped

public class SecurityService implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Context
	SecurityContext sc;

	@Inject
	private HttpServletRequest request;

	@Produces
	public AccessToken getAccessToken() {
		return ((KeycloakPrincipal) request.getUserPrincipal()).getKeycloakSecurityContext().getToken();
	}

	KeycloakSecurityContext kc = null;

	Map<String, Object> user = new HashMap<String, Object>();

	static boolean importMode = false;

	@PostConstruct
	public void init() {
		if (!importMode) {
			user.put("username", ((KeycloakPrincipal) request.getUserPrincipal()).getKeycloakSecurityContext()
					.getToken().getPreferredUsername());
			user.put("realm", ((KeycloakPrincipal) request.getUserPrincipal()).getKeycloakSecurityContext().getRealm());
			user.put("email", ((KeycloakPrincipal) request.getUserPrincipal()).getKeycloakSecurityContext().getToken()
					.getEmail());
			user.put("name",
					((KeycloakPrincipal) request.getUserPrincipal()).getKeycloakSecurityContext().getToken().getName());
			;

		}

	}

	public Boolean inRole(final String role) {
		if (!importMode) {
		return ((KeycloakPrincipal) request.getUserPrincipal()).getKeycloakSecurityContext().getToken().getRealmAccess()
				.isUserInRole(role);
		} else {
			return true;
		}
	}

	public String getRealm() {
		//return CoreEntity.DEFAULT_REALM;
		 if (!importMode) {
			 return ((KeycloakPrincipal)
		 request.getUserPrincipal()).getKeycloakSecurityContext().getRealm();
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

	public String getUserCode()
	{
		String username = (String)getUserMap().get("username");
		return "PER_"+QwandaUtils.getNormalisedUsername(username).toUpperCase();
	}
	
	public static void setImportMode(final boolean mode) {
		importMode = mode;
	}

	public String getToken() {
		return ((KeycloakPrincipal) request.getUserPrincipal()).getKeycloakSecurityContext().getTokenString();

	}

}
