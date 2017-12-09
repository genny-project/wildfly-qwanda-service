package life.genny.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;

@ApplicationScoped
public class SecureResources {

	/**
	 * @return the keycloakJsonMap
	 */
	public static Map<String, String> getKeycloakJsonMap() {
		return keycloakJsonMap;
	}

	private static Map<String, String> keycloakJsonMap = new HashMap<String, String>();
	private static String hostIP = System.getenv("HOSTIP") != null ? System.getenv("HOSTIP") : "127.0.0.1";
	private static String realmDir = System.getenv("REALM_DIR") != null ? System.getenv("REALM_DIR") : "./realm";

	public void init(@Observes @Initialized(ApplicationScoped.class) final Object init) {
		readFilenamesFromDirectory(realmDir);
	}

	public void destroy(@Observes @Destroyed(ApplicationScoped.class) final Object init) {
		keycloakJsonMap.clear();
	}

	public static void readFilenamesFromDirectory(final String rootFilePath) {
		final File folder = new File(rootFilePath);
		final File[] listOfFiles = folder.listFiles();
		final String localIP = System.getenv("HOSTIP");
		System.out.println("Loading Files! with HOSTIP=" + localIP);

		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				System.out.println("Importing Keycloak Realm File " + listOfFiles[i].getName());
				try {
					String keycloakJsonText = getFileAsText(listOfFiles[i]);
					// Handle case where dev is in place with localhost

					// if (!"localhost.json".equalsIgnoreCase(listOfFiles[i].getName())) {
					keycloakJsonText = keycloakJsonText.replaceAll("localhost", localIP);

					// }
					final String key = listOfFiles[i].getName(); // .replaceAll(".json", "");
					System.out.println("keycloak key:" + key + "," + keycloakJsonText);

					keycloakJsonMap.put(key, keycloakJsonText);
				} catch (final IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

			} else if (listOfFiles[i].isDirectory()) {
				System.out.println("Directory " + listOfFiles[i].getName());
				readFilenamesFromDirectory(listOfFiles[i].getName());
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
