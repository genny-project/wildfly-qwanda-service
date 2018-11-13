package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import io.swagger.annotations.Api;
import io.vertx.core.json.JsonObject;
import life.genny.qwanda.message.QDataRegisterMessage;
import life.genny.qwanda.service.Service;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.KeycloakUtils;
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

  @POST
  @Path("/register")
  @Consumes("application/json")
  @Produces("application/json")
  public Response register(final QDataRegisterMessage registration) {

    final String realm = GennySettings.dynamicRealm(registration.getRealm());
    final String jsonFile = realm + ".json";
    String userToken = null;

    String userId = null;
    final String token = service.getServiceToken(realm);
    log.info("Service token  = "+token+"\n");
    if (token != null) {

      try {
        userId = KeycloakUtils.register(token, registration);
        log.info("AccessToken for "+registration+" = "+userId);

        if (SecureResources.getKeycloakJsonMap().isEmpty()) {
          secureResources.init(null);
        }
        String keycloakJson = SecureResources.getKeycloakJsonMap().get(jsonFile);
        if (keycloakJson == null) {
          log.info("No keycloakMap for " + realm+" ... fixing");
          final String gennyKeycloakJson = SecureResources.getKeycloakJsonMap().get("genny");
          if (GennySettings.DEV_MODE) {
            SecureResources.getKeycloakJsonMap().put(jsonFile, gennyKeycloakJson);
            keycloakJson = gennyKeycloakJson;
          } else {
            log.info("Error - No keycloak Json file available for realm - "+realm);
            return null;
          }
        } else {
          log.info("keycloakMap for " + realm+" ."+keycloakJson);
        }
        final JsonObject realmJson = new JsonObject(keycloakJson);
        final JsonObject secretJson = realmJson.getJsonObject("credentials");
        final String secret = secretJson.getString("secret");
        log.info("secret " + secret);
        userToken = KeycloakUtils.getToken(registration.getKeycloakUrl(), realm, realm, secret, registration.getUsername(), registration.getPassword());
        log.info("User token = "+userToken);
      } catch (final IOException e) {
        return Response.status(400).entity("could not obtain access token").build();
      }

      class TokenClass  {
      }
      final TokenClass tokenObj = new TokenClass();
      return Response.status(200).entity(tokenObj).build();
    } else {
      return Response.status(400).entity("could not obtain token").build();
    }
  }

}
