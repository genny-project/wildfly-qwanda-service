package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.javamoney.moneta.Money;
import org.keycloak.representations.AccessTokenResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;


import io.swagger.annotations.Api;
import io.vertx.core.json.JsonObject;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.qwandautils.SecurityUtils;
import life.genny.security.SecureResources;

/**
 * JAX-RS endpoint
 *
 * @author Adam Crow
 */

@Path("/utils")
@Api(value = "/service", description = "Qwanda Service Utils API", tags = "qwandaservice,utils")
@Produces(MediaType.APPLICATION_JSON)

@RequestScoped
public class UtilsEndpoint {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	Boolean devMode = "TRUE".equals(System.getenv("GENNYDEV"));
	
	  String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");

	@PersistenceContext
	private EntityManager em;

	@Inject
	private Service service;

	@Inject
	private SecurityService securityService;

	public static class HibernateLazyInitializerSerializer extends JsonSerializer<JavassistLazyInitializer> {

		@Override
		public void serialize(final JavassistLazyInitializer initializer, final JsonGenerator jsonGenerator,
				final SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
			jsonGenerator.writeNull();
		}
	}

	

	
	@GET
	@Path("/token/{keycloakurl}/{realm}/{secret}/{key}/{initVector}/{username}/{encryptedPassword}")
	@Produces("application/json")
	@Transactional
	public Response getToken(@PathParam("keycloakurl") final String keycloakUrl,
			@PathParam("realm") final String realm,@PathParam("secret") final String secret, @PathParam("key") final String key,
			@PathParam("initVector") final String initVector,@PathParam("username") final String username,@PathParam("encryptedPassword") final String encryptedPassword) {
		
		AccessTokenResponse accessToken=null;
		try {
			accessToken = KeycloakUtils.getAccessToken(keycloakUrl, realm, realm,
					secret, username, encryptedPassword);
		} catch (IOException e) {
			return Response.status(400).entity("Could not obtain token").build();
		}
		String token = accessToken.getToken();

		
		return Response.status(200).entity(token).build();
	}
	
	static String env_security_key = System.getenv("ENV_SECURITY_KEY");

	@GET
	@Consumes("application/json")
	@Path("/subscribe/{projectcode}/{encryptedsubscriptiondata}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response processSubscription(@PathParam("projectcode") final String projectcode,@PathParam("encryptedsubscriptiondata") final String encryptedsubscriptiondata) {

		// convert projectcode to realm
		String realm = projectcode.substring("PRJ_".length()).toLowerCase();
		
		// decrypt the data
		String initVector = "PRJ_" + realm.toUpperCase();
		initVector = StringUtils.rightPad(initVector, 16, '*');
		
		String messageString = SecurityUtils.decrypt(env_security_key, initVector, encryptedsubscriptiondata);
		// update the subscription attributes for the user
		JsonObject json = new JsonObject(messageString);
		
		String usercode = json.getString("code");	
		String encProjectCode = json.getString("projectCode");
		if (!(projectcode.equalsIgnoreCase(encProjectCode))) {
			return Response.status(204).build();
		}
		BaseEntity user = null;
		try {
			user = service.findBaseEntityByCode(usercode);
		} catch (NoResultException e) {
			return Response.status(204).build();
		}	     
		
		Optional<EntityAttribute> username = user.findEntityAttribute("PRI_USERNAME");
		if (username.isPresent()) {
			// now get the service token
			try {
				String encryptedPassword = System.getenv("ENV_SERVICE_PASSWORD");
				
				String service_password = SecurityUtils.decrypt(env_security_key, initVector, encryptedPassword);
				// Now determine for the keycloak to use from the realm
				final String keycloakJsonText = SecureResources.getKeycloakJsonMap().get(realm + ".json");
				JsonObject keycloakJson  = new JsonObject(keycloakJsonText);
				String keycloakUrl = keycloakJson.getString("auth-server-url");
				String secret = keycloakJson.getJsonObject("credentials").getString("secret");
				String token = KeycloakUtils.getToken(keycloakUrl, realm, realm,
						secret, "service", service_password);
				log.info("token = "+token);
				QwandaUtils.apiPostEntity(bridgeApi, json.toString(), token);
				
			} catch (Exception e) {
				log.error("PRJ_" + realm.toUpperCase() + " attribute ENV_SERVIE_PASSWORD  is missing!");
			}
			
		}
		
	


		return Response.status(200).build();
	}

	private static final CurrencyUnit DEFAULT_CURRENCY_AUD = Monetary.getCurrency("AUD");
	
	@GET
	@Consumes("application/json")
	@Path("/stats")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response getStats() {
		
		// get number of buyers
		Integer buyers = 1513;
		// get number of transport companies
		Integer companies = 5759;
		// get total loads moved
		Integer jobs = 1449;
		// get paid to drivers in past 30 days
		Money money = Money.of(new BigDecimal("737298"), DEFAULT_CURRENCY_AUD);		


		
		return Response.status(200).build();
	}

}
