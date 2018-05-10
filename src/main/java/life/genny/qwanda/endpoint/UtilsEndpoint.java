package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.keycloak.representations.AccessTokenResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.swagger.annotations.Api;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;
import life.genny.qwandautils.KeycloakUtils;

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
	
	

	
}
