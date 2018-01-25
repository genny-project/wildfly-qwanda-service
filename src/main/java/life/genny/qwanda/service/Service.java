package life.genny.qwanda.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import life.genny.qwanda.DateTimeDeserializer;
import life.genny.qwanda.Link;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.message.QEventLinkChangeMessage;
import life.genny.qwanda.message.QEventSystemMessage;
import life.genny.qwanda.util.PersistenceHelper;
import life.genny.qwanda.util.WildFlyJmsQueueSender;
import life.genny.qwanda.util.WildflyJms;
import life.genny.qwandautils.QwandaUtils;
import life.genny.services.BaseEntityService2;

//@ApplicationScoped
//@Singleton
@RequestScoped
// @SessionScoped
// @Transactional
// @Lock(LockType.READ)
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

	String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");

	@PostConstruct
	public void init() {

	}

	@Override
	@javax.ejb.Asynchronous
	public void sendQEventAttributeValueChangeMessage(final QEventAttributeValueChangeMessage event) {
		// Send a vertx message broadcasting an attribute value Change
		System.out.println("!!ATTRIBUTE CHANGE EVENT ->" + event);
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer());
		Gson gson = gsonBuilder.create();

		try {
			String json = gson.toJson(event);
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
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer());
		Gson gson = gsonBuilder.create();

		try {
			String json = gson.toJson(event);
			QwandaUtils.apiPostEntity(bridgeApi, json, event.getToken());
		} catch (Exception e) {
			log.error("Error in posting link Change to JMS:" + event);
		}

	}

	@Override
	@javax.ejb.Asynchronous
	public void sendQEventSystemMessage(final String systemCode) {
		Properties properties = new Properties();
		try {
			properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
		} catch (IOException e) {

		}

		sendQEventSystemMessage(systemCode, properties, securityService.getToken());
	}

	@Override
	@javax.ejb.Asynchronous
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
		GsonBuilder gsonBuilder = new GsonBuilder();
		gsonBuilder.registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer());
		Gson gson = gsonBuilder.create();

		QEventSystemMessage event = new QEventSystemMessage(systemCode, properties, token);

		try {
			String json = gson.toJson(event);
			QwandaUtils.apiPostEntity(bridgeApi, json, token);
		} catch (Exception e) {
			log.error("Error in posting link Change to JMS:" + event);
		}

	}

	@Override
	@Lock(LockType.READ)
	public Long findChildrenByAttributeLinkCount(@NotNull final String sourceCode, final String linkCode,
			final MultivaluedMap<String, String> params) {
		return super.findChildrenByAttributeLinkCount(sourceCode, linkCode, params);
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
	}

	@Override
	@Transactional
	public Long update(final BaseEntity baseEntity) {
		return super.update(baseEntity);
	}
}
