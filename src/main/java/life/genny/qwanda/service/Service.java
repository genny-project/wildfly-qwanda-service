package life.genny.qwanda.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import life.genny.qwanda.DateTimeDeserializer;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.util.PersistenceHelper;
import life.genny.qwandautils.QwandaUtils;
import life.genny.services.BaseEntityService2;

@ApplicationScoped
@Singleton
// @SessionScoped
@Lock(LockType.READ)
public class Service extends BaseEntityService2 {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	public Service() {
		// TODO Auto-generated constructor stub
	}

	@Inject
	private PersistenceHelper helper;

	@Inject
	private SecurityService securityService;

	GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer());
	String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");

	@PostConstruct
	public void init() {

		this.setEm(helper.getEntityManager());
	}

	@Override
	@javax.ejb.Asynchronous
	public void sendQEventAttributeValueChangeMessage(final QEventAttributeValueChangeMessage event) {
		// Send a vertx message broadcasting an attribute value Change
		log.info("ATTRIBUTE CHANGE EVENT!" + event.getTargetBaseEntityCode() + ":" + event.getNewValue());
		Gson gson = gsonBuilder.create();
		try {
			QwandaUtils.apiPostEntity(bridgeApi, gson.toJson(event), event.getToken());
		} catch (IOException e) {

			log.error(
					"Error in posting to Vertx bridge:" + event.getTargetBaseEntityCode() + ":" + event.getNewValue());
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

}
