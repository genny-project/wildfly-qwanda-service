package life.genny.qwanda.service;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.validation.constraints.NotNull;
import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.representations.AccessToken;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import life.genny.qwanda.DateTimeDeserializer;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwanda.util.PersistenceHelper;
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
		// TODO Auto-generated constructor stub
	}

	// @PersistenceContext(unitName = "genny-persistence-unit")
	// private EntityManager em2;

	@Inject
	private PersistenceHelper helper;

	@Inject
	private SecurityService securityService;

	GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer());
	String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");

	@PostConstruct
	public void init() {

		// this.setEm(helper.getEntityManager());
	}

	@Override
	@javax.ejb.Asynchronous
	public void sendQEventAttributeValueChangeMessage(final QEventAttributeValueChangeMessage event) {
		// Send a vertx message broadcasting an attribute value Change
		System.out.println("!!!!!!ATTRIBUTE CHANGE EVENT!!!!!!!" + event.getAnswer().getTargetCode() + ":"
				+ event.getAnswer().getValue() + " token=" + StringUtils.abbreviateMiddle(event.getToken(), "...", 30));
		Gson gson = gsonBuilder.create();
		try {
			QwandaUtils.apiPostEntity(bridgeApi, gson.toJson(event), event.getToken());
		} catch (IOException e) {

			log.error("Error in posting to Vertx bridge:" + event.getAnswer().getValue() + " -> was "
					+ event.getOldValue());
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
		AccessToken token2 = securityService.getAccessToken();
		System.out.println(token2);
		return token;
	}

	@Override
	protected EntityManager getEntityManager() {
		return helper.getEntityManager();
		// return em2;
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
	public Long insert(final BaseEntity entity) {
		if (securityService.isAuthorised()) {
			String realm = securityService.getRealm();
			// System.out.println("Realm = " + realm);
			entity.setRealm(realm); // always override
			return super.insert(entity);
		}

		return null; // TODO throw Exception
	}

	@Override
	protected String getRealm() {
		return securityService.getRealm();
	}
}
