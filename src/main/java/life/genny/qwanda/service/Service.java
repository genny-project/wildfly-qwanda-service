package life.genny.qwanda.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Properties;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;

import io.vertx.core.json.JsonObject;
import life.genny.qwanda.Link;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.message.QEventLinkChangeMessage;
import life.genny.qwanda.message.QEventSystemMessage;
import life.genny.qwanda.util.PersistenceHelper;
import life.genny.qwanda.util.WildFlyJmsQueueSender;
import life.genny.qwanda.util.WildflyJms;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.KeycloakUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.qwandautils.SecurityUtils;
import life.genny.security.SecureResources;
import life.genny.services.BaseEntityService2;

@RequestScoped

public class Service extends BaseEntityService2 {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public Service() {

	}

	@Inject
	private PersistenceHelper helper;

	@Inject
	private SecurityService securityService;

	@Inject
	private WildFlyJmsQueueSender jms;

	@Inject
	private WildflyJms jms2;

	@Inject
	private Hazel inDB;

	String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");
	
	String token="DUMMY";

	@PostConstruct
	public void init() {
		
	}
	
	public void initServiceToken() {
		

		log.info("Initialising realm - "+GennySettings.mainrealm);
		token = getServiceToken(GennySettings.mainrealm);
	}
	
	@Override
	public String getToken()
	{
		if ("DUMMY".equals(token)) {
			init();
		}
		return token;
	}

	@Override
	@javax.ejb.Asynchronous
	public void sendQEventAttributeValueChangeMessage(final QEventAttributeValueChangeMessage event) {
		// Send a vertx message broadcasting an attribute value Change
		System.out.println("!!ATTRIBUTE CHANGE EVENT ->" + event);

		try {
			String json = JsonUtils.toJson(event);
			QwandaUtils.apiPostEntity(bridgeApi, json, event.getToken());
		} catch (Exception e) {
			log.error("Error in posting attribute changeto JMS:" + event);
		}

	}

	@Override
	@javax.ejb.Asynchronous
	public void sendQEventLinkChangeMessage(final QEventLinkChangeMessage event) {
		// Send a vertx message broadcasting an link Change
		System.out.println("!!LINK CHANGE EVENT ->" + event);

		BaseEntity originalParent  = null;
		BaseEntity targetParent = null;
		try {
			
			// update cache for source and target
			if (event.getOldLink()!=null) {
				if (event.getOldLink().getSourceCode()!=null) {
					String originalParentCode = event.getOldLink().getSourceCode();
					originalParent = this.findBaseEntityByCode(originalParentCode);
					updateDDT(originalParent.getCode(),JsonUtils.toJson(originalParent));
					QEventAttributeValueChangeMessage parentEvent = new QEventAttributeValueChangeMessage(originalParent.getCode(),originalParent.getCode(),originalParent,event.getToken());
					this.sendQEventAttributeValueChangeMessage(parentEvent);
				}
			}
			
			if (event.getLink()!=null) {
				if (event.getLink().getSourceCode()!=null) {
					String targetParentCode = event.getLink().getSourceCode();
					targetParent = this.findBaseEntityByCode(targetParentCode);
					updateDDT(targetParent.getCode(),JsonUtils.toJson(targetParent));
					QEventAttributeValueChangeMessage targetEvent = new QEventAttributeValueChangeMessage(targetParent.getCode(),targetParent.getCode(),targetParent,event.getToken());
					this.sendQEventAttributeValueChangeMessage(targetEvent);

				}
			}
			
			
			String json = JsonUtils.toJson(event);
			QwandaUtils.apiPostEntity(bridgeApi, json, event.getToken());
		} catch (Exception e) {
			log.error("Error in posting link Change to JMS:" + event);
		}

	}

	@Override
	public void sendQEventSystemMessage(final String systemCode) {
		Properties properties = new Properties();
		try {
			properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
		} catch (IOException e) {

		}

		sendQEventSystemMessage(systemCode, properties, securityService.getToken());
	}

	@Override
	public void sendQEventSystemMessage(final String systemCode, final String token) {
		Properties properties = new Properties();
		try {
			properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
		} catch (IOException e) {

		}
		sendQEventSystemMessage(systemCode, properties, token);
	}

	@Override
	@javax.ejb.Asynchronous
	public void sendQEventSystemMessage(final String systemCode, final Properties properties, final String token) {
		// Send a vertx message broadcasting an link Change
		System.out.println("!!System EVENT ->" + systemCode);

		QEventSystemMessage event = new QEventSystemMessage(systemCode, properties, token);

		try {
			String json = JsonUtils.toJson(event);
			QwandaUtils.apiPostEntity(bridgeApi, json, token);
		} catch (Exception e) {
			log.error("Error in posting link Change to JMS:" + event);
		}

	}

	@Lock(LockType.READ)
	public Long findChildrenByAttributeLinkCount(@NotNull final String sourceCode, final String linkCode,
			final MultivaluedMap<String, String> params) {

		return super.findChildrenByAttributeLinkCount(sourceCode, linkCode, params, null);
	}

	@Override
	@Lock(LockType.READ)
	public Long findChildrenByAttributeLinkCount(@NotNull final String sourceCode, final String linkCode,
			final MultivaluedMap<String, String> params, final String stakeholderCode) {

		return super.findChildrenByAttributeLinkCount(sourceCode, linkCode, params, stakeholderCode);
	}

	@Override
	protected String getCurrentToken() {
		String token = securityService.getToken();
		return token;
	}

	@Override
	protected EntityManager getEntityManager() {
		return helper.getEntityManager();
	}

	@Override
	public BaseEntity getUser() {
		BaseEntity user = null;
		String username = (String) securityService.getUserMap().get("username");
		final MultivaluedMap params = new MultivaluedMapImpl();
		params.add("PRI_USERNAME", username);

		List<BaseEntity> users = this.findBaseEntitysByAttributeValues(params, true, 0, 1);

		if (!((users == null) || (users.isEmpty()))) {
			user = users.get(0);

		}
		return user;
	}

	@Override
	@Transactional
	public Long insert(final BaseEntity entity) {
		if (securityService.isAuthorised()) {
			String realm = securityService.getRealm();
			entity.setRealm(realm); // always override
			return super.insert(entity);
		}

		return null; // TODO throw Exception
	}

	@Override
	@Transactional
	public EntityEntity addLink(final String sourceCode, final String targetCode, final String linkCode,
			final Object value, final Double weight) {
		try {
			return super.addLink(sourceCode, targetCode, linkCode, value, weight);
		} catch (IllegalArgumentException | BadDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	@Override
	@Transactional
	public void removeLink(final Link link) {
		super.removeLink(link);
	}

	@Override
	protected String getRealm() {
		return securityService.getRealm();
	//	return "genny"; // TODO HACK
	}

	@Override
	@Transactional
	public Long update(final BaseEntity baseEntity) {
		return super.update(baseEntity);
	}

	@Override
	public Boolean inRole(final String role) {
		return securityService.inRole(role);
	}

	// This was created to avoid sending large volumes of cache saves when in dev
	// mode
	@Override
	public void writeToDDT(final List<BaseEntity> bes) {


		if (GennySettings.devMode) {
			this.initServiceToken();
			int pageSize = 100;
			int pages = bes.size()/pageSize;
			for (int page=0;page<=pages;page++) {
			try {
				int arrSize = pageSize;
				if (page==pages) {
					arrSize = bes.size()-(page*pageSize);
				}
			
				
				BaseEntity[] arr = new BaseEntity[arrSize];
				for (int index=0;index<arrSize;index++) {
					int offset = (page*pageSize)+index;
					arr[index] = bes.get(offset);
				}
				System.out.println("Sending "+page+" to cache api");
				QDataBaseEntityMessage msg = new QDataBaseEntityMessage(arr, "CACHE",
						token);
				String jsonMsg = JsonUtils.toJson(msg);
				JsonObject json = new JsonObject();
				json.put("json", jsonMsg);
				QwandaUtils.apiPostEntity(GennySettings.ddtUrl + "/writearray", json.toString(), token);
				
			} catch (IOException e) {
				log.error("Could not write to cache");
			}
			}
		} else {
			for (BaseEntity be : bes) {
				writeToDDT(be);
			}
		}

	}

	@Override
	public void writeToDDT(final BaseEntity be) {
		String json = JsonUtils.toJson(be);
		writeToDDT(be.getCode(), json);

	}

	@Override
	@javax.ejb.Asynchronous
	public void writeToDDT(final String key, final String jsonValue) {
		final String realmKey = this.getRealm() + "_" + key;
		if (!GennySettings.isDdtHost) {
			if (!securityService.importMode) {
				try {
					
					JsonObject json = new JsonObject();
					json.put("key", key);
					json.put("json", jsonValue);
					QwandaUtils.apiPostEntity(GennySettings.ddtUrl + "/write", json.toString(), token);

				} catch (IOException e) {
					log.error("Could not write to cache");
				}
			}
		} else { // production or docker
			if (GennySettings.devMode) {
			try {
					
					JsonObject json = new JsonObject();
					json.put("key", key);
					json.put("json", jsonValue);
					QwandaUtils.apiPostEntity(GennySettings.ddtUrl + "/write", json.toString(), token);

				} catch (IOException e) {
					log.error("Could not write to cache");
				}
			
			} else {
			if (jsonValue == null) {
				inDB.getMapBaseEntitys(GennySettings.mainrealm).remove(key);
			} else {
				inDB.getMapBaseEntitys(GennySettings.mainrealm).put(key, jsonValue);
			}
			}
		}
		log.debug("Written to cache :" + key + ":" + jsonValue);
	}

	@Override
	@javax.ejb.Asynchronous
	public void updateDDT(final String key, final String value) {
		writeToDDT(key, value);
	}

	@Override
	public String readFromDDT(final String key) {
		final String realmKey = this.getRealm() + "_" + key;
		String json = (String) inDB.getMapBaseEntitys(GennySettings.mainrealm).get(realmKey);

		return json; // TODO make resteasy @Provider exception
	}

	@Override
	public void pushAttributes() {
		if (!securityService.importMode) {
			pushAttributesAsync();
		}
	}

	@javax.ejb.Asynchronous
	public void pushAttributesAsync() {
		// Attributes
		final List<Attribute> entitys = findAttributes();
		Attribute[] atArr = new Attribute[entitys.size()];
		atArr = entitys.toArray(atArr);
		QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
		msg.setToken(securityService.getToken());
		String json = JsonUtils.toJson(msg);
		writeToDDT("attributes", json);

	}
	
	public String getServiceToken(final String realm)
	{
		String key = null;
		String encryptedPassword = null;
		try {
			key = System.getenv("ENV_SECURITY_KEY"); // TODO , Add each realm as a prefix
		} catch (Exception e) {
			log.error("PRJ_" + realm.toUpperCase() + " ENV ENV_SECURITY_KEY  is missing!");
		}

		try {
			encryptedPassword = System.getenv("ENV_SERVICE_PASSWORD");
		} catch (Exception e) {
			log.error("PRJ_" + realm.toUpperCase() + " attribute ENV_SERVICE_PASSWORD  is missing!");
		}
		log.info("key:"+key+" and env_service_passwd="+encryptedPassword);
		return getServiceToken(realm,key,encryptedPassword);
	}
	
	public String getServiceToken(String realm, String key, String encrypted)
	{
		String ret = "DUMMY";
		String initVector = null;
		if (GennySettings.devMode) {  // UGLY!!!
			realm = "genny";
			initVector = "PRJ_GENNY*******";
			key = "WubbaLubbaDubDub";
			encrypted = "vRO+tCumKcZ9XbPWDcAXpU7tcSltpNpktHcgzRkxj8o=";

		} else {
		
			initVector = "PRJ_" + realm.toUpperCase();
			initVector = StringUtils.rightPad(initVector, 16, '*');
		}
		log.info("initVector:"+initVector);
		if (SecureResources.getKeycloakJsonMap().isEmpty()) {
			SecureResources.reload();
		} else {
//			for (String realmKey : SecureResources.getKeycloakJsonMap().keySet()) {
//				//System.out.println("json key field = "+realmKey);
//			}
		}
		String keycloakJson =  SecureResources.getKeycloakJsonMap().get(realm + ".json");
		if (keycloakJson!=null) {
		JsonObject realmJson = new JsonObject(keycloakJson);
		JsonObject secretJson = realmJson.getJsonObject("credentials");
		String secret = secretJson.getString("secret");
		log.info("secret:"+secret);


		String password = SecurityUtils.decrypt(key, initVector, encrypted);
		//log.info("password:"+password);
		// Now ask the bridge for the keycloak to use
		String keycloakurl = realmJson.getString("auth-server-url").substring(0,
				realmJson.getString("auth-server-url").length() - ("/auth".length()));

		try {
			ret = KeycloakUtils.getToken(keycloakurl, realm, realm, secret, "service", password);
		//	log.info("token = " + ret);



		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		} else {
			log.info("keycloakJson missing "+realm+".json");
		}
		return ret;
	}
	
	public String getKeycloakUrl(String realm)
	{
		String keycloakurl = null;
		
		if (GennySettings.devMode) {  // UGLY!!!
			realm = "genny";
		} 
		if (SecureResources.getKeycloakJsonMap().isEmpty()) {
			SecureResources.reload();
		} 
		String keycloakJson =  SecureResources.getKeycloakJsonMap().get(realm + ".json");
		if (keycloakJson!=null) {
		JsonObject realmJson = new JsonObject(keycloakJson);
		JsonObject secretJson = realmJson.getJsonObject("credentials");
		String secret = secretJson.getString("secret");
		log.info("secret:"+secret);

		keycloakurl = realmJson.getString("auth-server-url").substring(0,
				realmJson.getString("auth-server-url").length() - ("/auth".length()));
		}

		return keycloakurl;
	}
}
