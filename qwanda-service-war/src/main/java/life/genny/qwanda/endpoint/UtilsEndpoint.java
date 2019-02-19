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
			accessToken = KeycloakUtils.getAccessTokenResponse(keycloakUrl, realm, realm,
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
				String token = KeycloakUtils.getAccessToken(keycloakUrl, realm, realm,
						secret, "service", service_password);
				log.info("token = "+token);
				QwandaUtils.apiPostEntity(GennySettings.vertxUrl, json.toString(), token);
				
			} catch (Exception e) {
				log.error("PRJ_" + realm.toUpperCase() + " attribute ENV_SERVICE_PASSWORD  is missing!");
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
		
		// get number of sellers
		Long buyers  = (Long)em.createQuery("SELECT distinct count(*) FROM BaseEntity be, EntityAttribute ea where ea.baseEntityCode=be.code and  be.code LIKE 'PER_%' and ea.attributeCode='PRI_IS_SELLER' and ea.valueBoolean=1 ")
				.getSingleResult();

		// get number of  companies
		Long companies  = (Long)em.createQuery("SELECT distinct count(*)  FROM BaseEntity be  where be.code LIKE 'CPY_%' ")
				.getSingleResult();

		// get total items moved
		Long items  = (Long)em.createQuery("SELECT distinct count(*)  FROM BaseEntity be, EntityAttribute ea where ea.baseEntityCode=be.code and  be.code LIKE 'BEG_%' and ea.attributeCode='PRI_IS_RELEASE_PAYMENT_DONE' and ea.valueBoolean=1 ")
				.getSingleResult();

		// get total available items moved
		Long availitems  = (Long)em.createQuery("SELECT distinct count(be)  FROM BaseEntity be, EntityEntity ee where ee.link.targetCode=be.code and ee.link.targetCode LIKE 'BEG_%' and ee.link.sourceCode='GRP_NEW_ITEMS'")
				.getSingleResult();

		
		// get paid to drivers in past month
		Calendar c = Calendar.getInstance();
		c.add(Calendar.MONTH, -1);// then one month
		java.util.Date utilDate = c.getTime();
		String pattern = "yyyy-MM-dd";
        SimpleDateFormat formatter = new SimpleDateFormat(pattern);
        String mysqlDateString = formatter.format(utilDate);
		List<BaseEntity> results  = em.createQuery("SELECT distinct be  FROM BaseEntity be, EntityAttribute ea where ea.baseEntityCode=be.code and  be.code LIKE 'BEG_%' and ea.attributeCode='PRI_IS_RELEASE_PAYMENT_DONE' and ea.valueBoolean=1  and ea.updated > "+mysqlDateString)
				.getResultList();

		Money sum = Money.zero(DEFAULT_CURRENCY_AUD);
		
		for (BaseEntity mon : results) {
			Money value = mon.getValue("PRI_DRIVER_PRICE_INC_GST" , Money.zero(DEFAULT_CURRENCY_AUD));
			sum = sum.add(value);
		}
	

		QDataStatsMessage msg = new QDataStatsMessage(buyers.intValue(),companies.intValue(),items.intValue(),availitems.intValue(),sum);
		
		return Response.status(200).entity(msg).build();
	}

	@GET
	@Consumes("application/json")
	@Path("/project/{realm}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response getProject(@PathParam("realm") final String realm) {
		String projectCode = "PRJ_"+realm.toUpperCase();
		// get number of buyers
		Query q = em.createQuery("SELECT distinct (be) FROM BaseEntity be JOIN be.baseEntityAttributes bee where be.code=:code and bee.baseEntityCode=be.code");
				q.setParameter("code", projectCode);
				BaseEntity be = (BaseEntity)q.getSingleResult();
				Set<EntityAttribute> allowedAttributes = new HashSet<EntityAttribute>();
				for (EntityAttribute entityAttribute : be.getBaseEntityAttributes()) {
						String attributeCode = entityAttribute.getAttributeCode();
						switch (attributeCode) {
						case "PRI_COLOR":
						case "PRI_ONBOARDING_VIDEO":
						case "PRI_GREETING":
						case "PRI_LOGO":
						case "PRI_VERSION":
						case "PRI_IMAGE_URL":
						case "PRI_CODE":
						case "PRI_NAME":
							allowedAttributes.add(entityAttribute);
						default:
							}
	
				}
				be.setBaseEntityAttributes(allowedAttributes);
				String beString = JsonUtils.toJson(be);
		
		return Response.status(200).entity(beString).build();
	}
	
	@GET
    @Consumes("application/json")
    @Path("/baseentitycode/{username}")
    @Produces("application/json")
    public Response getUserCodeFromUserName(@PathParam("username") final String username) {
      log.info("Fetching base entity code for the user name " + username);
      String baseEntityCode = QwandaUtils.getUserCodeFromUserName(username);
      String json = JsonUtils.toJson(baseEntityCode);
      return Response.status(200).entity(json).build();
    }
	
}
