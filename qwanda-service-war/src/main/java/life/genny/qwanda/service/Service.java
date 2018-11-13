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
import life.genny.services.BatchLoading;

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
  private SecureResources secureResources;

  @Inject
  private Hazel inDB;

  String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");

  String token="DUMMY";

  @PostConstruct
  public void init() {

  }

  public void initServiceToken() {


    log.info("Initialising realm - "+GennySettings.MAIN_REALM);
    token = getServiceToken(GennySettings.MAIN_REALM);
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
      final String json = JsonUtils.toJson(event);
      QwandaUtils.apiPostEntity(bridgeApi, json, event.getToken());
    } catch (final Exception e) {
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
          final String originalParentCode = event.getOldLink().getSourceCode();
          originalParent = this.findBaseEntityByCode(originalParentCode);
          updateDDT(originalParent.getCode(),JsonUtils.toJson(originalParent));
          final QEventAttributeValueChangeMessage parentEvent = new QEventAttributeValueChangeMessage(originalParent.getCode(),originalParent.getCode(),originalParent,event.getToken());
          this.sendQEventAttributeValueChangeMessage(parentEvent);
        }
      }

      if (event.getLink()!=null) {
        if (event.getLink().getSourceCode()!=null) {
          final String targetParentCode = event.getLink().getSourceCode();
          targetParent = this.findBaseEntityByCode(targetParentCode);
          updateDDT(targetParent.getCode(),JsonUtils.toJson(targetParent));
          final QEventAttributeValueChangeMessage targetEvent = new QEventAttributeValueChangeMessage(targetParent.getCode(),targetParent.getCode(),targetParent,event.getToken());
          this.sendQEventAttributeValueChangeMessage(targetEvent);

        }
      }


      final String json = JsonUtils.toJson(event);
      QwandaUtils.apiPostEntity(bridgeApi, json, event.getToken());
    } catch (final Exception e) {
      log.error("Error in posting link Change to JMS:" + event);
    }

  }

  @Override
  public void sendQEventSystemMessage(final String systemCode) {
    final Properties properties = new Properties();
    try {
      properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
    } catch (final IOException e) {

    }

    sendQEventSystemMessage(systemCode, properties, securityService.getToken());
  }

  @Override
  public void sendQEventSystemMessage(final String systemCode, final String token) {
    final Properties properties = new Properties();
    try {
      properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
    } catch (final IOException e) {

    }
    sendQEventSystemMessage(systemCode, properties, token);
  }

  @Override
  @javax.ejb.Asynchronous
  public void sendQEventSystemMessage(final String systemCode, final Properties properties, final String token) {
    // Send a vertx message broadcasting an link Change
    System.out.println("!!System EVENT ->" + systemCode);

    final QEventSystemMessage event = new QEventSystemMessage(systemCode, properties, token);

    try {
      final String json = JsonUtils.toJson(event);
      QwandaUtils.apiPostEntity(bridgeApi, json, token);
    } catch (final Exception e) {
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
    final String token = securityService.getToken();
    return token;
  }

  @Override
  public EntityManager getEntityManager() {
    return helper.getEntityManager();
  }

  @Override
  public BaseEntity getUser() {
    BaseEntity user = null;
    final String username = (String) securityService.getUserMap().get("username");
    final MultivaluedMap params = new MultivaluedMapImpl();
    params.add("PRI_USERNAME", username);

    final List<BaseEntity> users = this.findBaseEntitysByAttributeValues(params, true, 0, 1);

    if (!((users == null) || (users.isEmpty()))) {
      user = users.get(0);

    }
    return user;
  }

  @Override
  @Transactional
  public Long insert(final BaseEntity entity) {
    if (securityService.isAuthorised()) {
      final String realm = securityService.getRealm();
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


    if (GennySettings.DEV_MODE) {
      this.initServiceToken();
      final int pageSize = 100;
      final int pages = bes.size()/pageSize;
      for (int page=0;page<=pages;page++) {
        try {
          int arrSize = pageSize;
          if (page==pages) {
            arrSize = bes.size()-(page*pageSize);
          }


          final BaseEntity[] arr = new BaseEntity[arrSize];
          for (int index=0;index<arrSize;index++) {
            final int offset = (page*pageSize)+index;
            arr[index] = bes.get(offset);
          }
          System.out.println("Sending "+page+" to cache api");
          final QDataBaseEntityMessage msg = new QDataBaseEntityMessage(arr, "CACHE",
              token);
          final String jsonMsg = JsonUtils.toJson(msg);
          final JsonObject json = new JsonObject();
          json.put("json", jsonMsg);
          QwandaUtils.apiPostEntity(GennySettings.DDT_URL + "/writearray", json.toString(), token);

        } catch (final IOException e) {
          log.error("Could not write to cache");
        }
      }
    } else {
      for (final BaseEntity be : bes) {
        writeToDDT(be);
      }
    }

  }

  @Override
  public void writeToDDT(final BaseEntity be) {
    final String json = JsonUtils.toJson(be);
    writeToDDT(be.getCode(), json);

  }

  @Override
  @javax.ejb.Asynchronous
  public void writeToDDT(final String key, final String jsonValue) {
    if (!GennySettings.IS_DDT_HOST) {
      if (!securityService.importMode) {
        try {

          final JsonObject json = new JsonObject();
          json.put("key", key);
          json.put("json", jsonValue);
          QwandaUtils.apiPostEntity(GennySettings.DDT_URL + "/write", json.toString(), token);

        } catch (final IOException e) {
          log.error("Could not write to cache");
        }
      }
    } else { // production or docker
      if (GennySettings.DEV_MODE) {
        try {

          final JsonObject json = new JsonObject();
          json.put("key", key);
          json.put("json", jsonValue);
          QwandaUtils.apiPostEntity(GennySettings.DDT_URL + "/write", json.toString(), token);

        } catch (final IOException e) {
          log.error("Could not write to cache");
        }

      } else {
        String dDTrealm = "genny";
        if ("genny".equalsIgnoreCase(securityService.getRealm())) {
          dDTrealm = GennySettings.MAIN_REALM;
        }
        if (jsonValue == null) {

          inDB.getMapBaseEntitys(dDTrealm).remove(key);
        } else {
          inDB.getMapBaseEntitys(dDTrealm).put(key, jsonValue);
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
    final String json = (String) inDB.getMapBaseEntitys(GennySettings.MAIN_REALM).get(realmKey);

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
    final QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
    msg.setToken(securityService.getToken());
    final String json = JsonUtils.toJson(msg);
    writeToDDT("attributes", json);

  }

  public  String getServiceToken(String realm) {
    log.info("Generating Service Token for "+realm);

    realm = GennySettings.dynamicRealm(realm);

    final String jsonFile = realm + ".json";

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
    }
    final JsonObject realmJson = new JsonObject(keycloakJson);
    final JsonObject secretJson = realmJson.getJsonObject("credentials");
    final String secret = secretJson.getString("secret");
    final String jsonRealm = realmJson.getString("realm");

    final String key = GennySettings.dynamicKey(jsonRealm);
    final String initVector = GennySettings.dynamicInitVector(jsonRealm);
    final String encryptedPassword = GennySettings.dynamicEncryptedPassword(jsonRealm);
    String password= null;


    log.info("key:"+key+":"+initVector+":"+encryptedPassword);
    password = SecurityUtils.decrypt(key, initVector, encryptedPassword);
    if (GennySettings.DEV_MODE) {
      password = GennySettings.DEFAULT_SERVICE_PASSWORD;
    }

    log.info("password="+password);

    // Now ask the bridge for the keycloak to use
    final String keycloakurl = realmJson.getString("auth-server-url").substring(0,
        realmJson.getString("auth-server-url").length() - ("/auth".length()));

    log.info(keycloakurl);

    try {
      log.info("realm() : " + realm + "\n" + "realm : " + realm + "\n" + "secret : " + secret + "\n"
          + "keycloakurl: " + keycloakurl + "\n" + "key : " + key + "\n" + "initVector : " + initVector + "\n"
          + "enc pw : " + encryptedPassword + "\n" + "password : " + password + "\n");

      final String token = KeycloakUtils.getToken(keycloakurl, realm, realm, secret, "service", password);
      log.info("token = " + token);
      return token;

    } catch (final Exception e) {
      log.info(e);
    }

    return null;
  }


  public String getKeycloakUrl(String realm)
  {
    String keycloakurl = null;

    if (GennySettings.DEV_MODE) {  // UGLY!!!
      realm = "genny";
    } 
    if (SecureResources.getKeycloakJsonMap().isEmpty()) {
      SecureResources.reload();
    } 
    final String keycloakJson =  SecureResources.getKeycloakJsonMap().get(realm + ".json");
    if (keycloakJson!=null) {
      final JsonObject realmJson = new JsonObject(keycloakJson);
      final JsonObject secretJson = realmJson.getJsonObject("credentials");
      final String secret = secretJson.getString("secret");
      log.info("secret:"+secret);

      keycloakurl = realmJson.getString("auth-server-url").substring(0,
          realmJson.getString("auth-server-url").length() - ("/auth".length()));
    }

    return keycloakurl;
  }

  @Transactional
  public void synchronizeSheetsToDataBase(String table) {
    final BatchLoading bl = new BatchLoading(this);
    bl.persistProject(true, table, false);
  }
}
