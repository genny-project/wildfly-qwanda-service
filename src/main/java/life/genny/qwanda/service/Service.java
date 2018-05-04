package life.genny.qwanda.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
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
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import com.google.gson.JsonObject;
import life.genny.qwanda.Link;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataAttributeMessage;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.message.QEventLinkChangeMessage;
import life.genny.qwanda.message.QEventSystemMessage;
import life.genny.qwanda.util.PersistenceHelper;
import life.genny.qwanda.util.WildFlyJmsQueueSender;
import life.genny.qwanda.util.WildflyJms;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;
import life.genny.services.BaseEntityService2;


@RequestScoped

public class Service extends BaseEntityService2 {

  /**
   * Stores logger object.
   */
  protected static final Logger log = org.apache.logging.log4j.LogManager
      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

  static final String ddtUrl = System.getenv("DDT_URL")==null?("http://" + System.getenv("HOSTIP") + ":8089"):System.getenv("DDT_URL");

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

  @PostConstruct
  public void init() {}

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

    try {
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
      properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties")
          .openStream());
    } catch (IOException e) {

    }

    sendQEventSystemMessage(systemCode, properties, securityService.getToken());
  }

  @Override
  public void sendQEventSystemMessage(final String systemCode, final String token) {
    Properties properties = new Properties();
    try {
      properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties")
          .openStream());
    } catch (IOException e) {

    }
    sendQEventSystemMessage(systemCode, properties, token);
  }

  @Override
  @javax.ejb.Asynchronous
  public void sendQEventSystemMessage(final String systemCode, final Properties properties,
      final String token) {
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
  public Long findChildrenByAttributeLinkCount(@NotNull final String sourceCode,
      final String linkCode, final MultivaluedMap<String, String> params) {

    return super.findChildrenByAttributeLinkCount(sourceCode, linkCode, params,null);
  }
  @Override
  @Lock(LockType.READ)
  public Long findChildrenByAttributeLinkCount(@NotNull final String sourceCode,
      final String linkCode, final MultivaluedMap<String, String> params, final String stakeholderCode) {

    return super.findChildrenByAttributeLinkCount(sourceCode, linkCode, params,stakeholderCode);
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
  public EntityEntity addLink(final String sourceCode, final String targetCode,
      final String linkCode, final Object value, final Double weight) {
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
  //  return securityService.getRealm();
    return "genny"; // TODO HACK
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

  @Override
	public void writeToDDT(final BaseEntity be)
	{  
	  String json = JsonUtils.toJson(be);
      writeToDDT(be.getCode(), json);

  }
  @Override
  @javax.ejb.Asynchronous
  public void writeToDDT(final String key, final String jsonValue) {
	final String realmKey = this.getRealm()+"_"+key;
	if ((System.getenv("GENNYDEV")!=null)&&(System.getenv("GENNYDEV").equalsIgnoreCase("TRUE"))) {
	    try {
      JsonObject json = new JsonObject();
      json.addProperty("key", key);
      json.addProperty("json", jsonValue);
       QwandaUtils.apiPostEntity(ddtUrl + "/write", json.toString(), "DUMMY");

    } catch (IOException e) {
      log.error("Could not write to cache");
    } 
	} else {  // production or docker
		if (jsonValue==null) {
			inDB.getMapBaseEntitys().remove(key);
		} else {
			inDB.getMapBaseEntitys().put(key, jsonValue);
		}
	}
     log.debug("Written to cache :"+key+":"+ jsonValue);
  }

  @Override
  @javax.ejb.Asynchronous
  public void updateDDT(final String key, final String value) {
    writeToDDT(key, value);
  }

  @Override
  public String readFromDDT(final String key) {
		final String realmKey = this.getRealm()+"_"+key;
	     String json = (String) inDB.getMapBaseEntitys().get(realmKey);
  

    return json; // TODO make resteasy @Provider exception
  }
  
  @Override
  public void pushAttributes()
  {
	  if (!securityService.importMode) {
		  pushAttributesAsync();
	  }
  }
  
  
  @javax.ejb.Asynchronous
  public void pushAttributesAsync()
  {
	    // Attributes
		final List<Attribute> entitys = findAttributes();
		Attribute[] atArr = new Attribute[entitys.size()];
		atArr = entitys.toArray(atArr);
		QDataAttributeMessage msg = new QDataAttributeMessage(atArr);
		msg.setToken(securityService.getToken());
		String json = JsonUtils.toJson(msg);
		writeToDDT("attributes",json);
	
  }
}
