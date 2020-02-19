package life.genny.qwanda.util;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;

import life.genny.qwandautils.GennySettings;

import life.genny.security.SecureResources;

public class PathBasedKeycloakConfigResolver implements KeycloakConfigResolver {
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	private final Map<String, KeycloakDeployment> cache = new ConcurrentHashMap<String, KeycloakDeployment>();
	private static String lastlog = "";

	@Override
	public KeycloakDeployment resolve(final OIDCHttpFacade.Request request) {
		URL aURL = null;
		String realm = "genny";
		String username = null;
		String key = "genny.json";

		if (request != null) {
			try {
				// Now check for a token

				if (request.getHeader("Authorization") != null) {

					// extract the token
					final String authTokenHeader = request.getHeader("Authorization");
					final String bearerToken = authTokenHeader.substring(7);
					// now extract the realm
					JSONObject jsonObj = null;
					String decodedJson = null;
					try {
						final String[] jwtToken = bearerToken.split("\\.");
						final Base64 decoder = new Base64(true);
						final byte[] decodedClaims = decoder.decode(jwtToken[1]);
						decodedJson = new String(decodedClaims);
						jsonObj = new JSONObject(decodedJson);
					} catch (final JSONException e1) {
						log.error(
								"bearerToken=" + bearerToken + "  decodedJson=" + decodedJson + ":" + e1.getMessage());
					}
					try {
						username = (String) jsonObj.get("preferred_username");
						realm = (String) jsonObj.get("azp");
						key = realm + ".json";
					} catch (final JSONException e1) {
						log.error("no customercode incuded with token for " + username + ":" + decodedJson);
					} catch (final NullPointerException e2) {
						log.error("NullPointerException for " + bearerToken + "::::::" + username + ":" + decodedJson);
					}

				} else {

					if (request.getURI().equals("http://localhost:8080/version")) {
						realm = GennySettings.mainrealm;
					} else {
					aURL = new URL(request.getURI());
					final String url = aURL.getHost();
					final String keycloakJsonText = SecureResources.getKeycloakJsonMap().get(url + ".json");
					if (keycloakJsonText==null) {
						log.error(url + ".json is NOT in qwanda-service Keycloak Map!");
						
					} else {
					// extract realm
					final JSONObject json = new JSONObject(keycloakJsonText);
					realm = json.getString("realm");
					key = realm + ".json";
					}
					}
				}

			} catch (final Exception e) {
				log.error("Error in accessing request.getURI , spi issue? "+e.getLocalizedMessage());
			}
		}

		// don't bother showing Docker health checks
		if (!request.getURI().equals("http://localhost:8080/version")) {
			String logtext = ">>>>> INCOMING REALM IS " + realm + " :" + request.getURI() + ":" + request.getMethod()
			+ ":" + request.getRemoteAddr();
			if (!logtext.equals(lastlog)) {
				log.debug(logtext);
				lastlog = logtext;
			}
		} else {
			Optional<String> firstRealm = SecureResources.getKeycloakJsonMap().keySet().stream().findFirst();
			if (firstRealm.isPresent()) {
				String kcStr = firstRealm.get();
				final String keycloakJsonText = SecureResources.getKeycloakJsonMap().get(kcStr);

				final JSONObject json = new JSONObject(keycloakJsonText);
				realm = json.getString("realm");
				key = realm + ".json";

			} else {
				log.error("No Realms in KeycloakJson Cache!");
				return null;
			}
		}

		KeycloakDeployment deployment = cache.get(realm);  // just get one
		key = realm;

		if (null == deployment) {
			InputStream is;
			try {
				String keycloakJson = SecureResources.getKeycloakJsonMap().get(key);
				if (keycloakJson != null) {
				is = new ByteArrayInputStream(
						keycloakJson.getBytes(StandardCharsets.UTF_8.name()));
				deployment = KeycloakDeploymentBuilder.build(is);
				cache.put(realm, deployment);
				} else {
					log.warn("Incorrect realm being used! - "+key);
				}
			} catch (final java.lang.RuntimeException ce) {
				ce.printStackTrace();
				log.debug("Connection Refused:"+username+":"+ realm + " :" + request.getURI() + ":" + request.getMethod()
				+ ":" + request.getRemoteAddr()+" ->"+ce.getMessage());
			} catch (final UnsupportedEncodingException e) {
				e.printStackTrace();
			}

		}

		return deployment;
	}

}
