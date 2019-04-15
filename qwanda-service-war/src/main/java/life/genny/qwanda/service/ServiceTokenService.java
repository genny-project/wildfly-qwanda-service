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
import org.glassfish.json.JsonUtil;
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
import life.genny.services.BaseEntityService2;
import life.genny.services.BatchLoading;
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

@ApplicationScoped

public class ServiceTokenService {
	
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	private HashMap<String,String> serviceTokens = new HashMap<String,String>();
	private HashMap<String,String> refreshServiceTokens = new HashMap<String,String>();
	

	
	@Inject
	private Service service;


	@PostConstruct
	public void init() {
		log.info("Initialising Service Token " + GennySettings.mainrealm);
	
		String mainRealmToken = generateServiceToken(GennySettings.mainrealm);
		
		serviceTokens.put(GennySettings.mainrealm,mainRealmToken);
		
	}

	public  String getServiceToken(String realm) {
		/* we get the service token currently stored in the cache */
		
		realm = GennySettings.mainrealm;
		
		/*if (GennySettings.devMode) {
			realm = "genny";
		} else {
			if ("genny".equals(realm)) {
				realm = GennySettings.mainrealm;
			}
		}*/
		
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

			/* if the difference is negative it means the expiry time is less than the nowTime 
				 if the difference < ACCESS_TOKEN_EXPIRY_LIMIT_SECONDS, it means the token will expire in 3 hours
			*/
			if(duration >= GennySettings.ACCESS_TOKEN_EXPIRY_LIMIT_SECONDS) {

			//	log.info("======= USING CACHED ACCESS TOKEN ========");

				/* if the token is NOTn about to expire (> 3 hours), we reuse it */
				return serviceToken;
			}
		}

		return generateServiceToken(realm);
	}
	
	public String generateServiceToken(String realm) {

		log.info("Generating Service Token for "+realm);
		
		realm = GennySettings.dynamicRealm(realm);

		JsonObject keycloakJson = VertxUtils.readCachedJson(GennySettings.mainrealm, GennySettings.KEYCLOAK_JSON);
		log.info("Keycloak JSON in generateServiceToken : " + keycloakJson);
		JsonObject secretJson;
		if (keycloakJson == null || "error".equals(keycloakJson.getString("status"))) {
			log.error("KEYCLOAK JSON NOT FOUND FOR " + realm);
			BatchLoading bl = new BatchLoading(service);
			String keycloakJsonText = bl.constructKeycloakJson();
			log.info("Keycloak JSON after construction: " + keycloakJsonText);
			keycloakJson = new JsonObject(keycloakJsonText);
			secretJson = keycloakJson.getJsonObject("credentials");
			
		} else {
			JsonObject keycloakValueJson = new JsonObject(keycloakJson.getString("value"));
			secretJson = keycloakValueJson.getJsonObject("credentials");
			keycloakJson = keycloakValueJson;
		}
		
		log.info("Secret JSON: " + secretJson);
		String secret = secretJson.getString("secret");
		String jsonRealm = keycloakJson.getString("realm");
		
		String key = GennySettings.dynamicKey(jsonRealm);
		String initVector = GennySettings.dynamicInitVector(jsonRealm);
		String encryptedPassword = GennySettings.dynamicEncryptedPassword(jsonRealm);
		String password= null;
		
		
		log.info("key:"+key+":"+initVector+":"+encryptedPassword);
		password = SecurityUtils.decrypt(key, initVector, encryptedPassword);
		if (GennySettings.devMode || GennySettings.miniKubeMode || (GennySettings.defaultLocalIP.equals(GennySettings.hostIP))) {
			password = GennySettings.defaultServicePassword;
		}

		log.info("password="+password);

		// Now ask the bridge for the keycloak to use
		String keycloakurl = keycloakJson.getString("auth-server-url").substring(0,
				keycloakJson.getString("auth-server-url").length() - "/auth".length());

		log.info(keycloakurl);

		try {
			log.info("realm()! : " + realm + "\n" + "realm! : " + realm + "\n" + "secret : " + secret + "\n"
					+ "keycloakurl: " + keycloakurl + "\n" + "key : " + key + "\n" + "initVector : " + initVector + "\n"
					+ "enc pw : " + encryptedPassword + "\n" + "password : " + password + "\n");

			/* we get the refresh token from the cache */
			String cached_refresh_token = null;
			if (refreshServiceTokens.containsKey(realm)) {
				cached_refresh_token = refreshServiceTokens.get(realm); 
			}	

			/* we get a secure token payload containing a refresh token and an access token */
			JsonObject secureTokenPayload = KeycloakUtils.getSecureTokenPayload(keycloakurl, realm, realm, secret, "service", password, cached_refresh_token);

			/* we get the access token and the refresh token */
			String access_token = secureTokenPayload.getString("access_token");
			String refresh_token = secureTokenPayload.getString("refresh_token");
			log.info("REFRESH TOKEN: " + refresh_token);

			/* if we have an access token */
			if (access_token != null) {
				
				serviceTokens.put(realm,access_token);
				refreshServiceTokens.put(realm,refresh_token);
				return access_token;
			}

		} catch (Exception e) {
			log.info(e);
		}

		return null;
	}
}