package life.genny.qwanda.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MultivaluedMap;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import io.vertx.core.json.JsonObject;
import life.genny.qwanda.Link;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.message.QEventLinkChangeMessage;
import life.genny.qwanda.message.QEventSystemMessage;
import life.genny.qwanda.util.PersistenceHelper;
import life.genny.qwanda.util.WildFlyJmsQueueSender;
import life.genny.qwanda.util.WildflyJms;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.qwandautils.SecurityUtils;
import life.genny.security.SecureResources;
import life.genny.services.BaseEntityService2;
import life.genny.utils.VertxUtils;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import life.genny.bootxport.bootx.RealmUnit;


@ApplicationScoped

public class ServiceTokenService {

	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	private HashMap<String, String> serviceTokens = new HashMap<String, String>();
	private HashMap<String, String> refreshServiceTokens = new HashMap<String, String>();

	@Inject
	private SecureResources secureResources;

	public void init(Map<String,Map> projects) {
		log.info("Initialising Service Tokens ");
	
		for (String projectCode : projects.keySet()) {
			Map<String,Object> project = projects.get(projectCode);
			log.info("Project: "+projects.get(projectCode));

			if ("FALSE".equals((String)project.get("disable"))) {
				String realm = projectCode;
				String keycloakUrl = (String)project.get("keycloakUrl");
				String secret = (String)project.get("clientSecret");
				String key = (String)project.get("ENV_SECURITY_KEY");
				String encryptedPassword = (String)project.get("ENV_SERVICE_PASSWORD");
				String realmToken = generateServiceToken(realm, keycloakUrl, secret, key, encryptedPassword);
		
				serviceTokens.put(projectCode,realmToken);
			}
		}
		
	}

	public void init(RealmUnit multitenancy) {
		log.info("Initialising Service Tokens ");
		 
			log.info("Project: "+multitenancy.getName());

			if (!multitenancy.getDisable()) {
				String realm = multitenancy.getCode();
				String keycloakUrl = multitenancy.getKeycloakUrl();
				String secret = multitenancy.getClientSecret();
				String key = multitenancy.getSecurityKey();
				String encryptedPassword = multitenancy.getServicePassword();
				String realmToken = generateServiceToken(realm, keycloakUrl, secret, key, encryptedPassword);
		
				serviceTokens.put(multitenancy.getCode(),realmToken);
			}
		
		
	}

	public String getToken(String realm) {
		return getServiceToken(realm); // I keep forgetting the name of the function so I'm creating this
	}
	
	public String getServiceToken(String realm) {
		/* we get the service token currently stored in the cache */

		if (GennySettings.devMode) {
			realm = "genny";
		} else {
			if ("genny".equals(realm)) {
				realm = GennySettings.mainrealm;
			}
		}

		String serviceToken = serviceTokens.get(realm);

		/* if we have got a service token cached */
		if (serviceToken != null) {

			/* we decode it */
			JSONObject decodedServiceToken = KeycloakUtils.getDecodedToken(serviceToken);

			/* we get the expiry timestamp */
			long expiryTime = decodedServiceToken.getLong("exp");

			/* we get the current time */
			long nowTime = LocalDateTime.now().atZone(TimeZone.getDefault().toZoneId()).toEpochSecond();

			/* we calculate the differencr */
			long duration = expiryTime - nowTime;

			/*
			 * if the difference is negative it means the expiry time is less than the
			 * nowTime if the difference < ACCESS_TOKEN_EXPIRY_LIMIT_SECONDS, it means the
			 * token will expire in 3 hours
			 */
			if (duration >= GennySettings.ACCESS_TOKEN_EXPIRY_LIMIT_SECONDS) {

				// log.info("======= USING CACHED ACCESS TOKEN ========");

				/* if the token is NOTn about to expire (> 3 hours), we reuse it */
				return serviceToken;
			}
		}

		return generateServiceToken(realm);
	}

	public String generateServiceToken(String realm) {

		log.info("Generating Service Token for " + realm);

		String jsonFile = realm + ".json";

		if (SecureResources.getKeycloakJsonMap().isEmpty()) {
			secureResources.init(null);
		}
		String keycloakJson = SecureResources.getKeycloakJsonMap().get(jsonFile);
		if (keycloakJson == null) {
			log.info("No keycloakMap for " + realm + " ... fixing");
			String gennyKeycloakJson = SecureResources.getKeycloakJsonMap().get("genny");
			if (GennySettings.devMode) {
				SecureResources.getKeycloakJsonMap().put(jsonFile, gennyKeycloakJson);
				keycloakJson = gennyKeycloakJson;
			} else {
				log.info("Error - No keycloak Json file available for realm - " + realm);
				return null;
			}
		}
		JsonObject realmJson = new JsonObject(keycloakJson);
		JsonObject secretJson = realmJson.getJsonObject("credentials");
		String secret = secretJson.getString("secret");
		String jsonRealm = realmJson.getString("realm");

		// Now ask the bridge for the keycloak to use
		String keycloakUrl = realmJson.getString("auth-server-url").substring(0,
				realmJson.getString("auth-server-url").length() - "/auth".length());

		String key = GennySettings.dynamicKey(jsonRealm);
		String initVector = GennySettings.dynamicInitVector(jsonRealm);
		String encryptedPassword = GennySettings.dynamicEncryptedPassword(jsonRealm);
		String password = null;

		return generateServiceToken(realm, keycloakUrl, secret, key, encryptedPassword);

	}

	public String generateServiceToken(final String realm, final String keycloakUrl, final String secret,
			final String key, final String encryptedPassword) {

		log.info("Generating Service Token for " + realm);

		String jsonFile = realm + ".json";

		String initVector = GennySettings.dynamicInitVector(realm);
		String password = null;

		log.info("key:" + key + ":" + initVector + ":" + encryptedPassword);
		password = SecurityUtils.decrypt(key, initVector, encryptedPassword);

		log.info("password=" + password);

		try {
			log.info("realm()! : " + realm + "\n" + "realm! : " + realm + "\n" + "secret : " + secret + "\n"
					+ "keycloakurl: " + keycloakUrl + "\n" + "key : " + key + "\n" + "initVector : " + initVector + "\n"
					+ "enc pw : " + encryptedPassword + "\n" + "password : " + password + "\n");

			/* we get the refresh token from the cache */
			String cached_refresh_token = null;
			if (refreshServiceTokens.containsKey(realm)) {
				cached_refresh_token = refreshServiceTokens.get(realm);
			}

			/*
			 * we get a secure token payload containing a refresh token and an access token
			 */
			JsonObject secureTokenPayload = KeycloakUtils.getSecureTokenPayload(keycloakUrl, realm, realm, secret,
					"service", password, cached_refresh_token);

			/* we get the access token and the refresh token */
			String access_token = secureTokenPayload.getString("access_token");
			String refresh_token = secureTokenPayload.getString("refresh_token");

			/* if we have an access token */
			if (access_token != null) {

				serviceTokens.put(realm, access_token);
				refreshServiceTokens.put(realm, refresh_token);
				return access_token;
			}

		} catch (Exception e) {
			log.info(e);
		}

		return null;
	}
}
