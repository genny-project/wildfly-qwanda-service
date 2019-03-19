package life.genny.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

import org.apache.logging.log4j.Logger;

import life.genny.qwandautils.GennySettings;

@ApplicationScoped
public class SecureResources {
	  protected static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	/**
	 * @return the keycloakJsonMap
	 */
	public static Map<String, String> getKeycloakJsonMap() {
		return keycloakJsonMap;
	}
	
	public static void setKeycloakJsonMap() {
		String keycloakJson = "{\n" + 
      	  		"  \"realm\": \"" + System.getenv("PROJECT_REALM") + "\",\n" + 
      	  		"  \"auth-server-url\": \"" + System.getenv("KEYCLOAKURL") + "\",\n" + 
      	  		"  \"ssl-required\": \"none\",\n" + 
      	  		"  \"resource\": \"" + System.getenv("PROJECT_REALM") + "\",\n" + 
      	  		"  \"credentials\": {\n" + 
      	  		"    \"secret\": \"" + System.getenv("KEYCLOAK_SECRET") + "\" \n" + 
      	  		"  },\n" + 
      	  		"  \"policy-enforcer\": {}\n" + 
      	  		"}";
            
      	  keycloakJsonMap.put("keycloak.json", keycloakJson);
	}

	private static Map<String, String> keycloakJsonMap = new HashMap<String, String>();



	public void init(@Observes @Initialized(ApplicationScoped.class) final Object init) {
		setKeycloakJsonMap();
	}

	public void destroy(@Observes @Destroyed(ApplicationScoped.class) final Object init) {
		keycloakJsonMap.clear();
	}

	public static void addRealm(final String key, String keycloakJsonText) {

		keycloakJsonText = keycloakJsonText.replaceAll("localhost", GennySettings.hostIP);
		log.info("Adding keycloak key:" + key + "," + keycloakJsonText);

		keycloakJsonMap.put(key, keycloakJsonText);
	}

	public static void removeRealm(final String key) {
		log.info("Removing keycloak key:" + key);

		keycloakJsonMap.remove(key);
	}

	public static void reload() {
		keycloakJsonMap.clear();
		setKeycloakJsonMap();
	}

	public static String fetchRealms() {
		String ret = "";
		for (String keycloakRealmKey : keycloakJsonMap.keySet()) {
			ret += keycloakRealmKey + ":" + keycloakJsonMap.get(keycloakRealmKey) + "\n";
		}
		return ret;
	}

	/*public static String readFilenamesFromDirectory(final String rootFilePath) {
		String ret = "";
		final File folder = new File(rootFilePath);
		final File[] listOfFiles = folder.listFiles();

		log.info("Loading Files! with HOSTIP=" + GennySettings.hostIP);

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				log.info("Importing Keycloak Realm File " + listOfFiles[i].getName());
				try {
					String keycloakJsonText = getFileAsText(listOfFiles[i]);
					// Handle case where dev is in place with localhost

					// if (!"localhost.json".equalsIgnoreCase(listOfFiles[i].getName())) {
					keycloakJsonText = keycloakJsonText.replaceAll("localhost", GennySettings.hostIP);

					// }
					final String key = listOfFiles[i].getName(); // .replaceAll(".json", "");
					log.info("keycloak key:" + key + "," + keycloakJsonText);

					keycloakJsonMap.put(key, keycloakJsonText);
					keycloakJsonMap.put(key+".json", keycloakJsonText);
					ret += keycloakJsonText + "\n";
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (listOfFiles[i].isDirectory()) {
				log.info("Directory " + listOfFiles[i].getName());
				readFilenamesFromDirectory(listOfFiles[i].getName());
			}
		}
		return ret;
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
	}*/
}
