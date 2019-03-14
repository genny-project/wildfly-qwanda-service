package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Currency;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.javamoney.moneta.Money;
import org.json.JSONObject;
import org.keycloak.representations.AccessTokenResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.swagger.annotations.Api;
import io.vertx.core.json.JsonObject;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QDataRegisterMessage;
import life.genny.qwanda.message.QDataStatsMessage;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.qwandautils.SecurityUtils;
import life.genny.security.SecureResources;

/**
 * JAX-RS endpoint
 *
 * @author Adam Crow
 */

@Path("/keycloak")
@Api(value = "/keycloak", description = "Qwanda Service Keycloak API", tags = "keycloak")
@Produces(MediaType.APPLICATION_JSON)

@RequestScoped
public class KeycloakEndpoint {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Inject
	private Service service;
	
	@Inject
	private SecureResources secureResources;

//	@POST
//	@Path("/register")
//	@Consumes("application/json")
//	@Produces("application/json")
//	public Response register(final QDataRegisterMessage registration) {
//
//		String realm = GennySettings.dynamicRealm(registration.getRealm());
//		String jsonFile = realm + ".json";
//		String userToken = null;
//
//		String userId = null;
//		String token = service.getServiceToken(realm);
//		log.info("Service token  = "+token+"\n");
//		if (token != null) {
//
//			try {
//				userId = KeycloakUtils.register(token, registration);
//				log.info("AccessToken for "+registration+" = "+userId);
//				
//				if (SecureResources.getKeycloakJsonMap().isEmpty()) {
//					secureResources.init(null);
//				}
//				String keycloakJson = SecureResources.getKeycloakJsonMap().get(jsonFile);
//				if (keycloakJson == null) {
//					log.info("No keycloakMap for " + realm+" ... fixing");
//					String gennyKeycloakJson = SecureResources.getKeycloakJsonMap().get("genny");
//					if (GennySettings.devMode) {
//						SecureResources.getKeycloakJsonMap().put(jsonFile, gennyKeycloakJson);
//						keycloakJson = gennyKeycloakJson;
//					} else {
//						log.info("Error - No keycloak Json file available for realm - "+realm);
//						return null;
//					}
//				} else {
//					log.info("keycloakMap for " + realm+" ."+keycloakJson);
//				}
//				JsonObject realmJson = new JsonObject(keycloakJson);
//				JsonObject secretJson = realmJson.getJsonObject("credentials");
//				String secret = secretJson.getString("secret");
//				log.info("secret " + secret);
//				userToken = KeycloakUtils.getAccessToken(registration.getKeycloakUrl(), realm, realm, secret, registration.getUsername(), registration.getPassword());
//				log.info("User token = "+userToken);
//			} catch (IOException e) {
//				return Response.status(400).entity("could not obtain access token").build();
//			}
//
//			class TokenClass  {
//					public String token;
//			}
//			TokenClass tokenObj = new TokenClass();
//			tokenObj.token = userToken;
//			return Response.status(200).entity(tokenObj).build();
//		} else {
//			return Response.status(400).entity("could not obtain token").build();
//		}
//	}

}
