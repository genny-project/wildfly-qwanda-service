package life.genny.security;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Destroyed;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;
import life.genny.qwandautils.GennySettings;
import life.genny.utils.VertxUtils;

@ApplicationScoped
public class SecureResources {
	  protected static final Logger log = org.apache.logging.log4j.LogManager
		      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	  
	  @Inject
	  private static SecurityService securityService;
	
	  public static String setKeycloakJsonMap() {
		final String REALM =  System.getenv("PROJECT_REALM");
		final String PROJECT_CODE = "PRJ_" + REALM.toUpperCase();
		String keycloakUrl = null;
		String keycloakSecret = null;
		
		BaseEntity project = VertxUtils.readFromDDT(REALM, PROJECT_CODE, securityService.getToken());
		if (project == null) {
			log.error("Error: no Project Setting for " + PROJECT_CODE);
		}
		Optional<EntityAttribute> entityAttribute1 = project.findEntityAttribute("ENV_KEYCLOAK_AUTHURL");
		if (entityAttribute1.isPresent()) {

			keycloakUrl = entityAttribute1.get().getValueString();

		} else {
			log.error("Error: no Project Setting for ENV_KEYCLOAK_AUTHURL ensure PRJ_" + REALM.toUpperCase()
					+ " has entityAttribute value for ENV_KEYCLOAK_AUTHURL");
			return null;
		}
		
		Optional<EntityAttribute> entityAttribute2 = project.findEntityAttribute("ENV_KEYCLOAK_SECRET");
		if (entityAttribute2.isPresent()) {

			keycloakSecret = entityAttribute2.get().getValueString();

		} else {
			log.error("Error: no Project Setting for ENV_KEYCLOAK_SECRET ensure PRJ_" + REALM.toUpperCase()
					+ " has entityAttribute value for ENV_KEYCLOAK_SECRET");
			return null;
		}
		
		String keycloakJson = "{\n" + 
      	  		"  \"realm\": \"" + GennySettings.mainrealm + "\",\n" + 
      	  		"  \"auth-server-url\": \"" + keycloakUrl + "\",\n" + 
      	  		"  \"ssl-required\": \"none\",\n" + 
      	  		"  \"resource\": \"" + GennySettings.mainrealm + "\",\n" + 
      	  		"  \"credentials\": {\n" + 
      	  		"    \"secret\": \"" + keycloakSecret + "\" \n" + 
      	  		"  },\n" + 
      	  		"  \"policy-enforcer\": {}\n" + 
      	  		"}";
            
      	  log.info("Loaded keycloak.json... " + keycloakJson);
      	  return keycloakJson;
	}




	  /*public static void init(@Observes @Initialized(ApplicationScoped.class) final Object init) {
	}

	public static void destroy(@Observes @Destroyed(ApplicationScoped.class) final Object init) {
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
		SecureResources.setKeycloakJsonMap();
	}

	public static String fetchRealms() {
		String ret = "";
		for (String keycloakRealmKey : keycloakJsonMap.keySet()) {
			ret += keycloakRealmKey + ":" + keycloakJsonMap.get(keycloakRealmKey) + "\n";
		}
		return ret;
	}*/

	
}
