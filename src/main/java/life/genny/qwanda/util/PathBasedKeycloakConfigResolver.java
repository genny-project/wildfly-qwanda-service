package life.genny.qwanda.util;


import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;
import org.keycloak.adapters.KeycloakConfigResolver;
import org.keycloak.adapters.KeycloakDeployment;
import org.keycloak.adapters.KeycloakDeploymentBuilder;
import org.keycloak.adapters.OIDCHttpFacade;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



public class PathBasedKeycloakConfigResolver implements KeycloakConfigResolver {
  /**
   * Stores logger object.
   */
  protected static final Logger log = org.apache.logging.log4j.LogManager
      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

  private static Map<String, String> keycloakJsonMap = new HashMap<String, String>();


  private final Map<String, KeycloakDeployment> cache =
      new ConcurrentHashMap<String, KeycloakDeployment>();

  @Override
  public KeycloakDeployment resolve(final OIDCHttpFacade.Request request) {
    URL aURL = null;
    String realm = "genny";
    String username = null;

    if (request != null) {
      // System.out.println("Keycloak Deployment Path incoming request:" + request);
      try {
        log.debug("Keycloak Deployment Path incoming request URI:" + request.getURI());
        // Now check for a token

        if (keycloakJsonMap.isEmpty()) {
          readFilenamesFromDirectory("./realm", keycloakJsonMap);
          log.debug("filenames loaded ...");
        } else {
          log.debug("filenames already loaded ...");
        }


        if (request.getHeader("Authorization") != null) {
          // extract the token
          final String authTokenHeader = request.getHeader("Authorization");
          log.debug("authTokenHeader:" + authTokenHeader);
          final String bearerToken = authTokenHeader.substring(7);
          log.debug("bearerToken:" + bearerToken);
          // now extract the realm
          JSONObject jsonObj = null;
          String decodedJson = null;
          try {
            final String[] jwtToken = bearerToken.split("\\.");
            final Base64 decoder = new Base64(true);
            final byte[] decodedClaims = decoder.decode(jwtToken[1]);
            decodedJson = new String(decodedClaims);
            jsonObj = new JSONObject(decodedJson);
            log.debug("******" + jsonObj);
          } catch (final JSONException e1) {
            log.error("bearerToken=" + bearerToken + "  decodedJson=" + decodedJson + ":"
                + e1.getMessage());
          }
          try {
            username = (String) jsonObj.get("preferred_username");
            realm = (String) jsonObj.get("aud");
          } catch (final JSONException e1) {
            log.error("no customercode incuded with token for " + username + ":" + decodedJson);
          } catch (final NullPointerException e2) {
            log.error("NullPointerException for " + bearerToken + "::::::" + username + ":"
                + decodedJson);
          }

        } else {

          aURL = new URL(request.getURI());
          final String url = aURL.getHost();
          log.debug("received KeycloakConfigResolver url:" + url);


          final String keycloakJsonText = keycloakJsonMap.get(url);
          log.debug("Selected KeycloakJson:[" + keycloakJsonText + "]");

          // extract realm
          final JSONObject json = new JSONObject(keycloakJsonText);
          log.debug("json:" + json);
          realm = json.getString("realm");
        }


      } catch (final Exception e) {
        log.error("Error in accessing request.getURI , spi issue?");
      }
    }


    log.info(">>>>> INCOMING REALM IS " + realm);

    KeycloakDeployment deployment = cache.get(realm);

    if (null == deployment) {
      InputStream is;
      try {
        is = new ByteArrayInputStream(
            keycloakJsonMap.get(realm).getBytes(StandardCharsets.UTF_8.name()));
        log.info("Building deployment ");
        deployment = KeycloakDeploymentBuilder.build(is);
        cache.put(realm, deployment);
      } catch (final UnsupportedEncodingException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }


    } else {
      log.debug("Deployment fetched from cache");
    }

    if (deployment != null) {
      log.debug("Deployment is not null ");
      log.debug("accountUrl:" + deployment.getAccountUrl());
      log.debug("realm:" + deployment.getRealm());
      log.debug("resource name:" + deployment.getResourceName());


    }

    return deployment;
  }

  private static void readFilenamesFromDirectory(final String rootFilePath,
      final Map<String, String> keycloakJsonMap) {
    final File folder = new File(rootFilePath);
    final File[] listOfFiles = folder.listFiles();
    final String localIP = System.getenv("HOSTIP");
    log.info("Loading Files! with HOSTIP=" + localIP);

    for (int i = 0; i < listOfFiles.length; i++) {
      if (listOfFiles[i].isFile()) {
        log.info("File " + listOfFiles[i].getName());
        try {
          String keycloakJsonText = getFileAsText(listOfFiles[i]);
          // Handle case where dev is in place with localhost

          // if (!"localhost.json".equalsIgnoreCase(listOfFiles[i].getName())) {
          keycloakJsonText = keycloakJsonText.replaceAll("localhost", localIP);

          // }
          final String key = listOfFiles[i].getName().replaceAll(".json", "");
          log.info("keycloak key:" + key + "," + keycloakJsonText);

          keycloakJsonMap.put(key, keycloakJsonText);
        } catch (final IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }

      } else if (listOfFiles[i].isDirectory()) {
        log.info("Directory " + listOfFiles[i].getName());
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

