package life.genny.qwanda.util;


import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



/**
 *
 * @author Juraci Paixão Kröhling <juraci at kroehling.de>
 */
public class PathBasedKeycloakConfigResolver implements KeycloakConfigResolver {

  private static Map<String, String> keycloakJsonMap = new HashMap<String, String>();


  private final Map<String, KeycloakDeployment> cache =
      new ConcurrentHashMap<String, KeycloakDeployment>();

  @Override
  public KeycloakDeployment resolve(final OIDCHttpFacade.Request request) {
    URL aURL = null;
    String realm = "wildfly-swarm-keycloak-example";
    String username = null;

    if (request != null) {
      System.out.println("Keycloak Deployment Path incoming request:" + request);
      try {
        System.out.println("Keycloak Deployment Path incoming request URI:" + request.getURI());
        // Now check for a token

        if (request.getHeader("Authorization") != null) {
          // extract the token
          final String authTokenHeader = request.getHeader("Authorization");
          System.out.println("authTokenHeader:" + authTokenHeader);
          final String bearerToken = authTokenHeader.substring(7);
          System.out.println("bearerToken:" + bearerToken);
          // now extract the realm
          JSONObject jsonObj = null;
          String decodedJson = null;
          try {
            final String[] jwtToken = bearerToken.split("\\.");
            final Base64 decoder = new Base64(true);
            final byte[] decodedClaims = decoder.decode(jwtToken[1]);
            decodedJson = new String(decodedClaims);
            jsonObj = new JSONObject(decodedJson);
            System.out.println("******" + jsonObj);
          } catch (final JSONException e1) {
            System.out.println("bearerToken=" + bearerToken + "  decodedJson=" + decodedJson + ":"
                + e1.getMessage());
          }
          try {
            username = (String) jsonObj.get("name");
          } catch (final JSONException e1) {
            System.out
                .println("no customercode incuded with token for " + username + ":" + decodedJson);
          } catch (final NullPointerException e2) {
            System.out.println("NullPointerException for " + bearerToken + "::::::" + username + ":"
                + decodedJson);
          }

        } else {

          aURL = new URL(request.getURI());
          final String url = aURL.getHost();
          System.out.println("received KeycloakConfigResolver url:" + url);

          if (!keycloakJsonMap.containsKey(url)) {
            System.out.println("Key (" + url + ") not in keycloakJsonMap ");
            readFilenamesFromDirectory("./realm", keycloakJsonMap);
            System.out.println("filenames loaded ...");
          } else {
            System.out.println("filenames already loaded ...");
          }

          final String keycloakJsonText = keycloakJsonMap.get(url);
          System.out.println("Selected KeycloakJson:[" + keycloakJsonText + "]");

          // extract realm
          final JSONObject json = new JSONObject(keycloakJsonText);
          System.out.println("json:" + json);
          realm = json.getString("realm");
        }


      } catch (final Exception e) {
        System.out.println("Error in accessing request.getURI , spi issue?");
      }
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

    System.out.println(">>>>> INCOMING REALM IS " + realm);

    KeycloakDeployment deployment = cache.get(realm);
    if (null == deployment) {
      // not found on the simple cache, try to load it from the file system
      final String fileName = System.getenv("JBOSS_HOME") + "/realm/" + realm + ".json";
      FileInputStream is;
      try {
        is = new FileInputStream(fileName);
        if (is == null) {
          throw new IllegalStateException("Not able to find the file /" + realm + ".json");
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

  private static void readFilenamesFromDirectory(final String rootFilePath,
      final Map<String, String> keycloakJsonMap) {
    final File folder = new File(rootFilePath);
    final File[] listOfFiles = folder.listFiles();

    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        System.out.println("File " + listOfFiles[i].getName());
        try {
          String keycloakJsonText = getFileAsText(listOfFiles[i]);
          // Handle case where dev is in place with localhost
          final String localIP = System.getenv("HOSTIP");
          keycloakJsonText = keycloakJsonText.replaceAll("localhost", localIP);
          keycloakJsonMap.put(listOfFiles[i].getName().replaceAll(".json", ""), keycloakJsonText);
        } catch (final IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      } else if (listOfFiles[i].isDirectory()) {
        System.out.println("Directory " + listOfFiles[i].getName());
        readFilenamesFromDirectory(listOfFiles[i].getName(), keycloakJsonMap);
      }
    }
  }

  private static String getFileAsText(final File file) throws IOException {
    final BufferedReader in = new BufferedReader(new FileReader(file));
    String ret = "";
    String line = null;
    while ((line = in.readLine()) != null) {
      ret += line;
    }
    in.close();

    return ret;
  }
}

