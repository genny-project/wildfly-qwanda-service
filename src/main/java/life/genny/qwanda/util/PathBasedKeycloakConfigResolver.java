package life.genny.qwanda.util;


import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



/**
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
public class PathBasedKeycloakConfigResolver implements KeycloakConfigResolver {


  private final Map<String, KeycloakDeployment> cache =
      new ConcurrentHashMap<String, KeycloakDeployment>();

  @Override
  public KeycloakDeployment resolve(final OIDCHttpFacade.Request request) {
    // final String path = request.getURI();
    if (request != null) {
      System.out.println("Keycloak Deployment Path incoming request:" + request);
      // try {
      // System.out.println("Keycloak Deployment Path incoming request URI:" + request.getURI());
      // } catch (final Exception e) {
      // System.out.println("Error in accessing request.getURI , spi issue?");
      // }
    }
    // final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    // final KeycloakSecurityContext session = (KeycloakSecurityContext) httpServletRequest
    // .getAttribute(KeycloakSecurityContext.class.getName());
    // final RefreshableKeycloakSecurityContext rf = (RefreshableKeycloakSecurityContext) session;
    // KeycloakDeployment deployment = rf.getDeployment();
    // // final AdapterDeploymentContext deploymentContext = (AdapterDeploymentContext)
    // servletContext
    // // .getAttribute(AdapterDeploymentContext.class.getName());
    // // final KeycloakDeployment deployment = deploymentContext.resolveDeployment(null);
    //
    // // final HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    // // final KeycloakSecurityContext kContext = (KeycloakSecurityContext) httpServletRequest
    // // .getAttribute(KeycloakSecurityContext.class.getName());
    // // String bearerToken = "";
    // //
    // // if (kContext != null) {
    // //
    // // JSONObject jsonObj = null;
    // // String decodedJson = null;
    // // try {
    // // bearerToken = kContext.getTokenString();
    // // final String[] jwtToken = bearerToken.split("\\.");
    // // final Base64 decoder = new Base64(true);
    // // final byte[] decodedClaims = decoder.decode(jwtToken[1]);
    // // decodedJson = new String(decodedClaims);
    // // jsonObj = new JSONObject(decodedJson);
    // // System.out.println(jsonObj);
    // // } catch (final JSONException e1) {
    // // // log.error("bearerToken=" + bearerToken + " decodedJson=" + decodedJson + ":" +
    // // // e1.getMessage());
    // // }
    // // }
    // // final int multitenantIndex = path.indexOf("multitenant/");
    // // if (multitenantIndex == -1) {
    // // throw new IllegalStateException("Not able to resolve realm from the request path!");
    // // }
    // //
    // // String realm = path.substring(path.indexOf("multitenant/")).split("/")[1];
    // // if (realm.contains("?")) {
    // // realm = realm.split("\\?")[0];
    // // }
    final String realm = "wildfly-swarm-keycloak-example";
    KeycloakDeployment deployment = cache.get(realm);
    if (null == deployment) {
      // not found on the simple cache, try to load it from the file system
      final String fileName = System.getenv("JBOSS_HOME") + "/realm/" + realm + "-keycloak.json";
      FileInputStream is;
      try {
        is = new FileInputStream(fileName);
        if (is == null) {
          throw new IllegalStateException("Not able to find the file /" + realm + "-keycloak.json");
        }
        System.out.println("Building deployment ");
        deployment = KeycloakDeploymentBuilder.build(is);
        cache.put(realm, deployment);
      } catch (final FileNotFoundException e) {
        e.printStackTrace();
      }

    } else {
      System.out.println("Deployment fetched from cache");
    }

    if (deployment != null) {
      System.out.println("Deployment is not null ");
      System.out.println("accountUrl:" + deployment.getAccountUrl());
      System.out.println("realm:" + deployment.getRealm());
      System.out.println("resource name:" + deployment.getResourceName());


    }

    return deployment;
  }

}

