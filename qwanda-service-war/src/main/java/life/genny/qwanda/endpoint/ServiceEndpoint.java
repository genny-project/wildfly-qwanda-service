package life.genny.qwanda.endpoint;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.TimeZone;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import org.jboss.ejb3.annotation.TransactionTimeout;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.json.JSONObject;
import life.genny.utils.VertxUtils;

import org.apache.logging.log4j.Logger;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRemoteException;
import org.eclipse.jgit.api.errors.TransportException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.errors.MissingObjectException;
import org.eclipse.jgit.errors.RevisionSyntaxException;
import org.eclipse.jgit.internal.storage.dfs.DfsRepositoryDescription;
import org.eclipse.jgit.internal.storage.dfs.InMemoryRepository;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectLoader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.RefSpec;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.treewalk.filter.AndTreeFilter;
import org.eclipse.jgit.treewalk.filter.PathFilter;
import org.eclipse.jgit.treewalk.filter.PathSuffixFilter;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.keycloak.representations.AccessTokenResponse;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import life.genny.qwanda.Answer;
import life.genny.qwanda.GPSLocation;
import life.genny.qwanda.GPSRoute;
import life.genny.qwanda.GPSRouteStatus;
impo
						import life.genny.qwanda.QuestionQuestion;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.controller.Controller;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataBaseEntityMessage;
im

	mport life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;
import life.genny.qwanda.validation.Validation;
import life.genny.qwandautils.GPSUtils;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.GitUtils;
import life.genny.qwandautils.JsonUtils;
impo
						import life.genny.qwandautils.QwandaUtils;

import life.genny.qwanda.controller.Controller;
import life.genny.security.SecureResources;

import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import life.genny.qwanda.AnswerLink;

/**
 * JAX-RS endpoint

			
@Path("/service")
@Ap
	(v
	lue = "/service", description = "Qwanda Service API", tags = "qwandaservice")
@Produces(MediaType.APPLICATION_JSON)

@RequestScoped
public class ServiceEndpoint {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");

	@PersistenceContext
	private EntityManager em;

	@Inject
	private Service service;

	@Inject
	private SecurityService securityService;

	public static class HibernateLazyInitializerSerializer extends JsonSerializer<JavassistLazyInitializer> {

		@Override
		public void serialize(final JavassistLazyInitializer initializer, final JsonGenerator jsonGenerator,
				final SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
			jsonGenerator.writeNull();
		}
	}

	@GET
	@Path("/token/{keycloakurl}/{realm}/{secret}/{key}/{initVector}/{username}/{encryptedPassword}")
	@Produces("application/json")
	@Transactional
	public Response getToken(@PathParam("keycloakurl") final String keycloakUrl, @PathParam("realm") final String realm,
			@PathParam("secret") final String secret, @PathParam("key") final String key,
			@PathParam("initVector") final String initVector, @PathParam("username") final String username,
			@PathParam("encryptedPassword") final String encryptedPassword) {

		AccessTokenResponse accessToken = null;
		try {
			accessToken = KeycloakUtils.getAccessTokenResponse(keycloakUrl, realm, realm, secret, username,
					encryptedPassword);
		} catch (IOException e) {
			return Response.status(400).entity("Could not obtain token").build();
		}
		String token = accessToken.getToken();

		return Response.status(200).entity(token).build();
	}

	@GET
	@Path("/baseentitys/importkeycloak")
	@Produces("application/json")
	@Transactional
	// user MUST BE SUPERADMIN
	public Response importKeycloakUsers(@QueryParam("keycloakurl") final String keycloakUrl,
			@QueryParam("realm") final String realm, @QueryParam("username") final String username,
			@QueryParam("password") final String password, @QueryParam("clientid") final String clientId,
			@QueryParam("max") final Integer max,
			@DefaultValue("GRP_USERS") @QueryParam("parentgroups") final String parentGroupCodes) {
		Long usersAddedCount = 0L;

		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			log.error("IMPORT KEYCLOAK DISABLED IN CODE");
			// service.importKeycloakUsers(keycloakUrl,
			// realm, username,
			// password, clientId,
			// max, parentGroupCodes);
		}
		return Response.status(200).entity(usersAddedCount).build();
	}

	@POST
	@Path("/baseentitys/uploadcsv")
	@Consumes("multipart/form-data")
	@Transactional
	@TransactionTimeout(value = 500, unit = TimeUnit.SECONDS)
	public Response uploadFile(final MultipartFormDataInput input) throws IOException {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			final Map<String, List<InputPart>> uploadForm = input.getFormDataMap();

			// Get file data to save
			final List<InputPart> inputParts = uploadForm.get("attachment");

			for (final InputPart inputPart : inputParts) {
				try {

					final MultivaluedMap<String, String> header = inputPart.getHeaders();
					final String fileName = getFileName(header);

					// convert the uploaded file to inputstream
					final InputStream inputStream = inputPart.getBody(InputStream.class, null);

					// byte[] bytes = IOUtils.toByteArray(inputStream);
					// constructs upload file path
					// writeFile(bytes, fileName);
					service.importBaseEntitys(inputStream, fileName);

					return Response.status(200).entity("Imported file name : " + fileName).build();

				} catch (final Exception e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	private String getFileName(final MultivaluedMap<String, String> header) {

		final String[] contentDisposition = header.getFirst("Content-Disposition").split(";");

		for (final String filename : contentDisposition) {
			if ((filename.trim().startsWith("filename"))) {

				final String[] name = filename.split("=");

				final String finalFileName = name[1].trim().replaceAll("\"", "");
				return finalFileName;
			}
		}
		return "unknown";
	}

	Controller ctl = new Controller();

	@GET
	@Path("/synchronize/cache/attributes")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@TransactionTimeout(value = 1500, unit = TimeUnit.SECONDS)
	public Response synchronizeCache() {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			service.pushAttributes();
		} else {
			return Response.status(401).entity("You need to be a dev.").build();
		}
		return Response.status(200).entity("ok").build();
	}

	@GET
	@Path("/synchronize/cache/baseentitys")
	@ApiOperation(value = "baseentitys", notes = "Sync all BaseEntitys")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	@TransactionTimeout(value = 1500, unit = TimeUnit.SECONDS)
	public Response synchronizeCacheBEs() {

		List<BaseEntity> results = new ArrayList<BaseEntity>();
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {
			log.info(" Writing BaseEntitys to cache.");
			results = em.createQuery("SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea ")
					.getResultList();
			for (BaseEntity be : results) {
				service.writeToDDT(be);
			}
		} else {
			return Response.status(401).entity("You need to be a dev.").build();
		}
		log.info(results.size() + " BaseEntitys written to cache.");
		return Response.status(200).entity(results.size() + " BaseEntitys written to cache.").build();
	}

	@GET
	@Path("/cache/read/{key}")
	@ApiOperation(value = "cache", notes = "read cache data located at Key")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response cacheRead(@PathParam("key") final String key) {
		String results = null;
		log.info("Cache Fetch for key=" + key);
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {
			log.info("Reading from cache : key = [" + key + "]");
			log.info("realm=[" + securityService.getRealm() + "]");
			// log.info("token=[" + service.getToken() + "]");
			results = service.readFromDDT(key);
		} else {
			return Response.status(400).entity("Access not allowed").build();
		}
		return Response.status(200).entity(results).build();
	}

	@POST
	@Path("/cache/write/{key}")
	@ApiOperation(value = "cache", notes = "write cache data located at Key")
	@Produces(MediaType.APPLICATION_JSON)
	public Response cacheWrite(@PathParam("key") final String key, final String data) {
		String results = null;
		log.info("Cache Write for key=" + key);
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {
			log.info("Writing into cache : key = [" + key + "] for realm " + securityService.getRealm());
			VertxUtils.writeCachedJson(securityService.getRealm(), key, data, service.getToken());
		} else {
			return Response.status(400).entity("Access not allowed").build();
		}
		return Response.status(200).build();
	}

	@GET
	@Path("/cache/clear")
	@ApiOperation(value = "cache", notes = "clear all cache data")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response cacheClear() {

		log.info("Cache Clear");
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {
			service.clearCache();
		} else {
			return Response.status(400).entity("Access not allowed").build();
		}
		return Response.status(200).build();
	}

	@GET
	@Path("/answers/{sourceCode}/{targetCode}/{attributeCode}/{value}")
	@ApiOperation(value = "answer", notes = "quick answer")
	@Produces(MediaType.APPLICATION_JSON)

	public Response setAnswer(@PathParam("sourceCode") final String sourceCode,
			@PathParam("targetCode") final String targetCode, @PathParam("attributeCode") final String attributeCode,
			@PathParam("value") final String value) {
		BaseEntity result = null;
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			Answer answer = new Answer(sourceCode, targetCode, attributeCode, value);
			Attribute attribute = service.findAttributeByCode(attributeCode);
			if (attribute == null) {
				log.error("Bad attribute supplied " + attributeCode);
			} else {
				answer.setAttribute(attribute);
				Answer[] answerArray = new Answer[1];
				answerArray[0] = answer;

				service.insert(answerArray);
				result = (BaseEntity) em
						.createQuery("SELECT be FROM BaseEntity be JOIN  be.baseEntityAttributes ea where be.code=:code")
						.setParameter("code", targetCode).getSingleResult();
			}

		}
		return Response.status(200).entity(result).build();
	}

	@GET
	@Path("/synchronize/cache/baseentitys/{baseEntityCode}")
	@ApiOperation(value = "baseentitys", notes = "Sync BaseEntity to cache")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response cacheSync(@PathParam("baseEntityCode") final String code) {
		BaseEntity result = null;
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			result = (BaseEntity) em
					.createQuery("SELECT be FROM BaseEntity be JOIN  be.baseEntityAttributes ea where be.code=:code")
					.setParameter("code", code).getSingleResult();
			service.writeToDDT(result);
		}
		return Response.status(200).entity(result).build();
	}

	@GET
	@Path("/baseentitys/{baseEntityCode}/hide")
	@ApiOperation(value = "baseentitys", notes = "Hide a baseentity by changing realm to hidden")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response hideBaseEntity(@PathParam("baseEntityCode") final String baseEntityCode) {
		BaseEntity result = null;
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			result = service.findBaseEntityByCode(baseEntityCode);
			result.setRealm("hidden");
			service.update(result);

		}
		return Response.status(200).entity(result).build();
	}

	@GET
	@Path("/gps/{origin}/{destination}/distance/{percentage}")
	@Produces("application/json")
	public Response fetchCurrentRouteStatusByPercentageDistance(@PathParam("origin") final String originAddress,
			@PathParam("destination") final String destinationAddress, @PathParam("percentage") final Double percentage) {
		String json = null;
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			String googleApiKey = System.getenv("GOOGLE_API_KEY");
			if (googleApiKey == null) {
				String realm = securityService.getRealm();
				String projectCode = "PRJ_" + realm.toUpperCase();
				BaseEntity project = service.findBaseEntityByCode(projectCode);
				Optional<EntityAttribute> ea = project.findEntityAttribute("PRI_GOOGLE_API_KEY");
				if (ea.isPresent()) {
					googleApiKey = ea.get().getValueString();
				}
			}

			if (googleApiKey != null) {
				GPSLocation origin = GPSUtils.getGPSLocation(originAddress, googleApiKey);
				GPSLocation end = GPSUtils.getGPSLocation(destinationAddress, googleApiKey);
				GPSRoute route = GPSUtils.getRoute(origin, end, googleApiKey);

				GPSRouteStatus status = GPSUtils.fetchCurrentRouteStatusByPercentageDistance(route, percentage);

				json = JsonUtils.toJson(status);
			} else {
				json = "{\"status\":\"error\",\"description\":\"No Google API\"}";
			}
		}
		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/gps/{origin}/{destination}/duration/{currentSeconds}")
	@Produces("application/json")
	public Response fetchCurrentRouteStatusByPercentageDuration(@PathParam("origin") final String originAddress,
			@PathParam("destination") final String destinationAddress,
			@PathParam("currentSeconds") final Double currentSeconds) {
		String json = null;
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			String googleApiKey = System.getenv("GOOGLE_API_KEY");
			if (googleApiKey == null) {
				String realm = securityService.getRealm();
				String projectCode = "PRJ_" + realm.toUpperCase();
				BaseEntity project = service.findBaseEntityByCode(projectCode);
				Optional<EntityAttribute> ea = project.findEntityAttribute("PRI_GOOGLE_API_KEY");
				if (ea.isPresent()) {
					googleApiKey = ea.get().getValueString();
				}
			}

			if (googleApiKey != null) {
				GPSLocation origin = GPSUtils.getGPSLocation(originAddress, googleApiKey);
				GPSLocation end = GPSUtils.getGPSLocation(destinationAddress, googleApiKey);
				GPSRoute route = GPSUtils.getRoute(origin, end, googleApiKey);

				GPSRouteStatus status = GPSUtils.fetchCurrentRouteStatusByDuration(route, currentSeconds);

				json = JsonUtils.toJson(status);
			} else {
				json = "{\"status\":\"error\",\"description\":\"No Google API\"}";
			}
		}
		return Response.status(200).entity(json).build();
	}

	@GET
	@Path("/baseentitys/remove/{code}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response removeBaseEntity(@PathParam("code") final String code) {
		String returnMessage = "";

		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {
			service.removeBaseEntity(code);

		} else {
			returnMessage = "Cannot find BE " + code;
		}

		return Response.status(200).entity(returnMessage).build();
	}

	@GET
	@Path("/rulegroup/{rulegroup}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response executeRuleGroup(@PathParam("rulegroup") final String rulegroup) {
		String returnMessage = "rule group  fired";

		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {
			String token = securityService.getToken();

			// Why did I make this mandatory? ACC
			Properties properties = new Properties();
			try {
				properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties").openStream());
			} catch (IOException e) {

			}
			log.info("Sending rulegroup - " + rulegroup + " from " + securityService.getUserMap().get("prefered_username"));
			QEventSystemMessage event = new QEventSystemMessage("FOCUS_RULE_GROUP", properties, token);
			event.getData().setValue(rulegroup);

			try {
				String json = JsonUtils.toJson(event);
				QwandaUtils.apiPostEntity(bridgeApi, json, token);
			} catch (Exception e) {
				log.error("Error in posting link Change to JMS:" + event);
			}

		} else {
			return Response.status(401).entity("You need to be a dev.").build();
		}

		return Response.status(200).entity(returnMessage).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/baseentitys")
	public Response create(final BaseEntity entity) {

		Long ret = null;

		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {
			BaseEntity be3 = null;
			String userCode = securityService.getUserCode();
			try {
				be3 = service.findBaseEntityByCode(entity.getCode());
				be3 = em.find(BaseEntity.class, be3.getId());
			} catch (NoResultException e2) {
				be3 = null;
			}
			BaseEntity entity2 = service.addAttributes(entity);
			if (be3 == null) {
				ret = service.insert(entity2);
			}
			if (ret == null) {
				ret = service.update(entity2);
			}
			// now save all the entityattribute
			List<Answer> answers = new ArrayList<Answer>();
			for (EntityAttribute ea : entity.getBaseEntityAttributes()) {
				Answer answer = new Answer(userCode, entity.getCode(), ea.getAttributeCode(), ea.getAsString());
				answer.setWeight(ea.getWeight());
				answer.setChangeEvent(false);
				answer.setInferred(ea.getInferred());
				answer.setAttribute(ea.getAttribute());
				answers.add(answer);
			}
			service.insert(answers.toArray(new Answer[answers.size()]));

			/*
			 * if it is a person AND it has a username then ensure that keycloak is updated
			 */

			BaseEntity be = null;

			try {
				be = service.findBaseEntityByCode(entity2.getCode(), true);
				if (be.getCode().startsWith("PER_")) {
					Optional<EntityAttribute> optUsername = be.findEntityAttribute("PRI_USERNAME");
					if (optUsername.isPresent()) {
						String newUsername = optUsername.get().getAsString();

						Optional<EntityAttribute> optFirstname = be.findEntityAttribute("PRI_FIRSTNAME");
						if (optFirstname.isPresent()) {
							String newFirstname = optFirstname.get().getAsString();

							Optional<EntityAttribute> optLastname = be.findEntityAttribute("PRI_LASTNAME");
							if (optLastname.isPresent()) {
								String newLastname = optLastname.get().getAsString();

								Optional<EntityAttribute> optEmail = be.findEntityAttribute("PRI_EMAIL");
								if (optEmail.isPresent()) {
									String newEmail = optEmail.get().getAsString();

									String serviceToken = service.getServiceToken(securityService.getRealm());

									// Now check if it exists in keycloak
									String keycloakUrl = service.getKeycloakUrl(securityService.getRealm());

									try {
										String keycloakUserId = KeycloakUtils.createUser(serviceToken, securityService.getRealm(),
												newUsername, newFirstname, newLastname, newEmail);
										log.info("KEYCLOAK USER ID: " + keycloakUserId);
										Answer keycloakIdAnswer = new Answer(be.getCode(), be.getCode(), "PRI_KEYCLOAK_UUID",
												keycloakUserId);
										be.addAnswer(keycloakIdAnswer);
										service.updateWithAttributes(be);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									} catch (BadDataException e) {
										// TODO Auto-generated catch block
										e.printStackTrace();
									}
								}
							}
						}

					}
				}
			} catch (NoResultException e1) {

			}

		}
		return Response.status(200).entity(ret).build();
	}

	@GET
	@Path("/realms/sync")
	@ApiOperation(value = "syncrealms", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response syncrealms() {
		log.debug("Sync Keycloak Realms");
		String keycloakRealms = SecureResources.reload();
		return Response.status(200).entity(keycloakRealms).build();
	}

	@GET
	@Path("/realms")
	@ApiOperation(value = "syncrealms", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchrealms() {
		log.debug("Fetch Keycloak Realms");
		String keycloakRealms = SecureResources.fetchRealms();
		return Response.status(200).entity(keycloakRealms).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/realms")
	@Transactional
	public Response createRealm(final String entity) {
		log.debug("Add Keycloak Realm");

		JSONObject json = new JSONObject(entity);
		String key = json.getString("clientId");
		key = key + ".json";

		SecureResources.addRealm(key, entity);

		return Response.created(UriBuilder.fromResource(QwandaEndpoint.class).build()).build();
	}

	@DELETE
	@Consumes("application/json")
	@Path("/realms")
	@Produces("application/json")
	@Transactional
	public Response removeRealm(final String key) {

		log.info("Removing Realm " + key);

		SecureResources.removeRealm(key);
		return Response.created(UriBuilder.fromResource(QwandaEndpoint.class).build()).build();
	}

	// cheap way of getting clean transaction
	@GET
	@Path("/synchronize/{table}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response synchronize(@PathParam("table") final String table) {

		String response = "Failed";

		try {
			response = QwandaUtils.apiGet(GennySettings.qwandaServiceUrl + "/qwanda/synchronizesheet/" + table,
					securityService.getToken(), 240);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return Response.status(200).entity(response).build();
	}

	/**
	 * Calls the synchronizeSheetsToDataBase method in the Service and returns the
	 * response.
	 *
	 * @param table
	 * @return response of the synchronization
	 */
	@GET
	@Path("/synchronizesheet/{table}")
	public String startSynchronization(@PathParam("table") final String table) {
		ctl.synchronizeSheetsToDataBase(service, table);
		return "Success";
	}

	/**
	 * Calls the syncLayouts method in the Service and returns the response.
	 *
	 * @param table
	 * @return response of the synchronization
	 */
	@GET
	@Consumes("application/json")
	@Path("/synchronizelayouts")
	@Transactional
	// @TransactionTimeout(value = 1000, unit = TimeUnit.SECONDS)
	public Response synchronizeLayouts(
			@DefaultValue("https://github.com") @QueryParam("giturl") final String gitserverUrl,
			@DefaultValue("genny-project") @QueryParam("accountname") final String accountname,
			@DefaultValue("layouts.git") @QueryParam("project") final String project,
			@DefaultValue("genny") @QueryParam("realm") final String realm,
			@DefaultValue("master") @QueryParam("branch") final String branch)
			throws BadDataException, InvalidRemoteException, TransportException, GitAPIException, RevisionSyntaxException,
			AmbiguousObjectException, IncorrectObjectTypeException, IOException {

		String ret = "Synced";

		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			String gitUrl = gitserverUrl + "/" + accountname + "/" + project;

			BaseEntity be3 = null;
			log.info("************* Generating V1 Layouts *************");

			log.info("Synchronizing Layouts");
			log.info("Realm = " + realm);
			log.info("gitUrl = " + gitUrl);
			log.info("Branch = " + branch);

			// V1

			List<BaseEntity> gennyLayouts = GitUtils.getLayoutBaseEntitys(gitUrl, branch, realm, "genny/sublayouts", true); // get
																																																											// common
																																																											// layouts

			log.info("about to synch sublayouts for genny");
			QDataSubLayoutMessage v1messages = synchLayouts(gennyLayouts, false);
			log.info("writing to cache GENNY-V1-LAYOUTS");
			VertxUtils.writeCachedJson(securityService.getRealm(), "GENNY-V1-LAYOUTS", JsonUtils.toJson(v1messages),
					service.getToken());

			List<BaseEntity> realmLayouts = GitUtils.getLayoutBaseEntitys(gitUrl, branch, realm, realm + "/sublayouts", true);
			QDataSubLayoutMessage v1realmmessages = synchLayouts(realmLayouts, false);
			log.info("writing to cache " + realm.toUpperCase() + "-V1-LAYOUTS");
			VertxUtils.writeCachedJson(securityService.getRealm(), realm.toUpperCase() + "-V1-LAYOUTS",
					JsonUtils.toJson(v1realmmessages), service.getToken());

			// Do V2

			log.info("************* Generating V2 Layouts *************");

			gennyLayouts = GitUtils.getLayoutBaseEntitys(gitUrl, branch, realm, "genny", false); // get common layouts
			// service.saveLayouts( gennyLayouts, "genny-new", "V2", branch);
			synchLayouts(gennyLayouts, true);
			realmLayouts = GitUtils.getLayoutBaseEntitys(gitUrl, branch, realm, realm + "-new", true);
			synchLayouts(realmLayouts, true);
			// service.saveLayouts( realmLayouts, realm+"-new", "V2", branch);

			List<BaseEntity> layouts = new ArrayList<BaseEntity>();
			// log.info("genny "+gennyLayouts.size()+" layouts ");
			layouts.addAll(gennyLayouts);
			// log.info(realm+" "+realmLayouts.size()+" layouts ");
			layouts.addAll(realmLayouts);
			//
			// for (BaseEntity layout : layouts) {
			// log.info("Loaded Layout " + layout.getCode()+"
			// "+layout.getName()+":"+layout.getValue("PRI_LAYOUT_URI").get().toString());
			// }
			//
			//
			// synchLayouts(layouts,true); // save the layouts to the database

			QDataBaseEntityMessage msg = new QDataBaseEntityMessage(layouts.toArray(new BaseEntity[0]));
			msg.setParentCode("GRP_LAYOUTS");
			msg.setLinkCode("LNK_CORE");
			VertxUtils.writeCachedJson(realm, "V2-LAYOUTS", JsonUtils.toJson(msg), service.getToken());

			log.info("Loaded in layouts for realm " + realm);
			// genny/sublayouts ..
			// [{"name":"DRIVER.json","download_url":"http://layout-cache:2223/genny/sublayouts/DRIVER.json","path":"/genny/sublayouts/DRIVER.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"LOAD-ITEM.json","download_url":"http://layout-cache:2223/genny/sublayouts/LOAD-ITEM.json","path":"/genny/sublayouts/LOAD-ITEM.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"LOAD.json","download_url":"http://layout-cache:2223/genny/sublayouts/LOAD.json","path":"/genny/sublayouts/LOAD.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"OFFER-ACCEPTED.json","download_url":"http://layout-cache:2223/genny/sublayouts/OFFER-ACCEPTED.json","path":"/genny/sublayouts/OFFER-ACCEPTED.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"OFFER.json","download_url":"http://layout-cache:2223/genny/sublayouts/OFFER.json","path":"/genny/sublayouts/OFFER.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"OWNER.json","download_url":"http://layout-cache:2223/genny/sublayouts/OWNER.json","path":"/genny/sublayouts/OWNER.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"STAFF.json","download_url":"http://layout-cache:2223/genny/sublayouts/STAFF.json","path":"/genny/sublayouts/STAFF.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"address-dropoff-display.json","download_url":"http://layout-cache:2223/genny/sublayouts/address-dropoff-display.json","path":"/genny/sublayouts/address-dropoff-display.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"address-pickup-display.json","download_url":"http://layout-cache:2223/genny/sublayouts/address-pickup-display.json","path":"/genny/sublayouts/address-pickup-display.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"list-item-conversation.json","download_url":"http://layout-cache:2223/genny/sublayouts/list-item-conversation.json","path":"/genny/sublayouts/list-item-conversation.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"list-item-website.json","download_url":"http://layout-cache:2223/genny/sublayouts/list-item-website.json","path":"/genny/sublayouts/list-item-website.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"load-action-buttons.json","download_url":"http://layout-cache:2223/genny/sublayouts/load-action-buttons.json","path":"/genny/sublayouts/load-action-buttons.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"map-display.json","download_url":"http://layout-cache:2223/genny/sublayouts/map-display.json","path":"/genny/sublayouts/map-display.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"price-display.json","download_url":"http://layout-cache:2223/genny/sublayouts/price-display.json","path":"/genny/sublayouts/price-display.json","modified_date":"2019-03-08T03:33:37.341Z"},{"name":"quote-received.json","download_url":"http://layout-cache:2223/genny/sublayouts/quote-received.json","path":"/genny/sublayouts/quote-received.json","modified_date":"2019-03-08T03:33:37.341Z"}]
		}
		return Response.status(200).entity(ret).build();
	}

	public QDataSubLayoutMessage synchLayouts(final List<BaseEntity> layouts, final boolean saveToDatabase) {

		Layout[] layoutArray = new Layout[layouts.size()];

		Attribute layoutDataAttribute = service.findAttributeByCode("PRI_LAYOUT_DATA");
		Attribute layoutURLAttribute = service.findAttributeByCode("PRI_LAYOUT_URL");
		Attribute layoutURIAttribute = service.findAttributeByCode("PRI_LAYOUT_URI");
		Attribute layoutNameAttribute = service.findAttributeByCode("PRI_LAYOUT_NAME");
		Attribute layoutModifiedDateAttribute = service.findAttributeByCode("PRI_LAYOUT_MODIFIED_DATE");

		int index = 0;
		for (BaseEntity layout : layouts) {
			final String code = layout.getCode();
			BaseEntity existingLayout = null;

			// log.info(e.getLocalizedMessage());
			// so save the layout
			BaseEntity newLayout = null;
			try {
				newLayout = service.findBaseEntityByCode(layout.getCode());
			} catch (NoResultException e) {
				log.info("New Layout detected");
			}
			if (newLayout == null) {
				newLayout = new BaseEntity(layout.getCode(), layout.getName());
			} else {
				int newData = layout.getValue("PRI_LAYOUT_DATA").get().toString().hashCode();
				Optional<EntityAttribute> oldDataEA = newLayout.findEntityAttribute("PRI_LAYOUT_DATA");
				if (oldDataEA.isPresent()) {
					int oldData = oldDataEA.get().getAsString().hashCode();
					if (newData == oldData) {
						continue;
					}
				}
			}
			newLayout.setRealm(securityService.getRealm());
			newLayout.setUpdated(layout.getUpdated());

			if (saveToDatabase) {
				service.upsert(newLayout);
			}

			try {
				EntityAttribute ea = newLayout.addAttribute(layoutDataAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_DATA").get().toString());
				EntityAttribute ea2 = newLayout.addAttribute(layoutURLAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_URL").get().toString());
				EntityAttribute ea3 = newLayout.addAttribute(layoutURIAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_URI").get().toString());
				EntityAttribute ea4 = newLayout.addAttribute(layoutNameAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_NAME").get().toString());
				EntityAttribute ea5 = newLayout.addAttribute(layoutModifiedDateAttribute, 0.0,
						layout.getValue("PRI_LAYOUT_MODIFIED_DATE").get().toString());
			} catch (final BadDataException e) {
				e.printStackTrace();
			}

			if (saveToDatabase) {
				try {

					// service.updateWithAttributes(newLayout);
					try {
						// merge in entityAttributes
						newLayout = service.getEntityManager().merge(newLayout);
					} catch (final Exception e) {
						// so persist otherwise
						service.getEntityManager().persist(newLayout);
					}
					String json = JsonUtils.toJson(newLayout);
					service.writeToDDT(newLayout.getCode(), json);
					service.addLink("GRP_LAYOUTS", newLayout.getCode(), "LNK_CORE", "LAYOUT", 1.0, false); // don't send
					// change
					// event
				} catch (IllegalArgumentException | BadDataException e) {
					log.error("Could not write layout - " + e.getLocalizedMessage());
				}

			}

			// create layout array
			try {
				layoutArray[index++] = new Layout(layout.getValue("PRI_LAYOUT_NAME").get().toString(),
						layout.getValue("PRI_LAYOUT_DATA").get().toString(), null, null, null);
			} catch (Exception e) {
			}

			String json = JsonUtils.toJson(newLayout);
			VertxUtils.writeCachedJson(newLayout.getRealm(), newLayout.getCode(), json, service.getToken());
			index++;

		}
		QDataSubLayoutMessage v1messages = new QDataSubLayoutMessage(layoutArray, service.getToken());

		log.info("Finished loading " + layouts.size() + " layouts");
		return v1messages;
	}

	/**
	 * Returns a CSV export of the realm
	 *
	 * @param table
	 * @return response of the synchronization
	 */
	@GET
	@Consumes("application/json")
	@Path("/export/{table}/{realm}")
	@Transactional
	public Response exportRealm(@PathParam("table") final String tableStr, @PathParam("realm") final String realmStr)
			throws BadDataException, InvalidRemoteException, TransportException, GitAPIException, RevisionSyntaxException,
			AmbiguousObjectException, IncorrectObjectTypeException, IOException {

		String ret = "";
		String realm = realmStr.toLowerCase().trim(); // TODO, check if realm is real
		String table = tableStr.toLowerCase().trim(); // TODO, check if table is real

		if (securityService.inRole("superadmin") || securityService.inRole("dev") || GennySettings.devMode) {

			log.info("************* Generating Realm Export *************");

			switch (table) {

			case "attribute":
				// final String code = (String) object.get("code");
				// final String name = (String) object.get("name");
				// final String dataType = (String) object.get("datatype");
				// final String privacy = (String) object.get("privacy");
				// final String description = (String) object.get("description");
				// final String help = (String) object.get("help");
				// final String placeholder = (String) object.get("placeholder");
				// final String defaultValue = (String) object.get("defaultValue");
				// BaseEntitys
				List<Attribute> attributes = em.createQuery("SELECT distinct att FROM Attribute att  where att.realm=:realm")
						.setParameter("realm", realm).getResultList();

				// code name datatype Hint (Example Associated BaseEntity) privacy description
				// help placeholder defaultValue
				// PRI_IS_INTERN Intern DTT_BOOLEAN USER
				ret = "\"code\",\"name\",\"datatype\",\"privacy\",\"description\",\"help\",\"placeholder\",\"defaultValue\"\n";
				for (Attribute att : attributes) {
					ret += cell(att.getCode()) + cell(att.getName()) + cell(att.getDataType().getClassName())
							+ cell(att.getDefaultPrivacyFlag()) + cell(att.getDescription()) + cell(att.getHelp())
							+ cell(att.getPlaceholder()) + cell(att.getDefaultValue()) + "\n";
				}

				log.info("Exported " + attributes.size() + " attributes for realm " + realm);
				break;

			case "baseentity":
				// BaseEntitys
				List<BaseEntity> bes = em.createQuery("SELECT distinct be FROM BaseEntity be  where be.realm=:realm")
						.setParameter("realm", realm).getResultList();

				// code name icon
				// GRP_DASHBOARD Dashboard dashboard
				ret = "\"code\",\"name\",\"icon\"\n";
				for (BaseEntity be : bes) {
					ret += "\"" + be.getCode() + "\",\"" + be.getName() + "\",\"\"\n";
				}

				log.info("Exported " + bes.size() + " bes for realm " + realm);
				break;
			case "entityattribute":
				// BaseEntitys
				List<EntityAttribute> eas = em.createQuery("SELECT ea FROM EntityAttribute ea where ea.realm=:realm")
						.setParameter("realm", realm).getResultList();

				// baseEntityCode attributeCode weight valueString valueDateTime valueLong
				// valueInteger valueDouble
				// PRJ_INTERNMATCH PRI_NAME 1 INTERNMATCH
				ret = "\"baseEntityCode\",\"attributeCode\",\"weight\",\"valueString\",\"valueDateTime\",\"valueLong\",\"valueInteger\",\"valueDouble\"\n";
				for (EntityAttribute ea : eas) {
					ret += cell(ea.getBaseEntityCode()) + cell(ea.getAttributeCode()) + cell(ea.getWeight())
							+ cell(ea.getValueString()) + cell(ea.getValueDateTime()) + cell(ea.getValueLong())
							+ cell(ea.getValueInteger()) + cell(ea.getValueDouble()) + "\n";
				}

				log.info("Exported " + eas.size() + " eas for realm " + realm);
				break;

			case "validation":
				// Validations
				List<Validation> vlds = em.createQuery("SELECT vld FROM Validation vld where vld.realm=:realm")
						.setParameter("realm", realm).getResultList();

				// code name regex group_codes recursive multi_allowed
				// VLD_SELECT_EDU_PROVIDER dropdown .* GRP_EDU_PROVIDER_SELECTION FALSE
				ret = "\"code\",\"name\",\"regex\",\"group_codes\",\"recursive\",\"multi_allowed\"\n";
				for (Validation a : vlds) {
					ret += cell(a.getCode()) + cell(a.getName()) + cell(a.getRegex()) + cell(a.getSelectionBaseEntityGroupList())
							+ cell(a.getRecursiveGroup()) + cell(a.getMultiAllowed())

							+ "\n";
				}

				log.info("Exported " + vlds.size() + " eas for realm " + realm);
				break;

			default:
				log.error("Unknown table [" + table + "]");
				return Response.status(400).build();
			}

		}
		return Response.status(200).entity(ret).build();
	}

	private String cell(final String field) {
		return "\"" + field + "\",";
	}

	private String cell(final boolean field) {
		return "\"" + (field ? "TRUE" : "FALSE") + "\",";
	}

	private String cell(final Double field) {
		return "\"" + (field.toString()) + "\",";
	}

	private String cell(final LocalDateTime field) {
		return "\"" + (field.toString()) + "\",";
	}

	private String cell(final Long field) {
		return "\"" + (field.toString()) + "\",";
	}

	private String cell(final Integer field) {
		return "\"" + (field.toString()) + "\",";
	}

	private String cell(final List<String> field) {
		return "\"" + (field.toString()) + "\",";
	}

	@GET
	@Path("/forms")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getForms() {

		if (securityService.inRole("superadmin") || securityService.inRole("dev") || securityService.inRole("test")) {

			String realm = securityService.getRealm();
			// select distinct(q.code) from question q LEFT JOIN question_question qq ON
			// q.code = qq.sourceCode WHERE qq.sourceCode IS NULL and q.realm='internmatch'
			// and q.code like 'QUE_%_GRP';
			try {
				Query q = em.createNativeQuery(
						"select distinct(q.code) from question q LEFT JOIN question_question qq ON q.code = qq.sourceCode WHERE qq.sourceCode IS NOT NULL and q.realm='"
								+ realm + "' and q.code like 'QUE_%_GRP'");

				List<Object[]> questionCodes = q.getResultList();
				// Query q2 = em.createNativeQuery("select distinct(q.code) from question q LEFT
				// JOIN question_question qq ON q.code = qq.sourceCode WHERE qq.sourceCode IS
				// NOT NULL and q.realm='"+realm+"' and q.code LIKE 'QUE_JOURNAL_W1_GRP' LIMIT
				// 1");
				// List<Object[]> questionCodes2 = q2.getResultList();
				// Query q3 = em.createNativeQuery("select distinct(q.code) from question q LEFT
				// JOIN question_question qq ON q.code = qq.sourceCode WHERE qq.sourceCode IS
				// NOT NULL and q.realm='"+realm+"' and q.code LIKE 'QUE_JOURNAL_W1D1_GRP' LIMIT
				// 1");
				// List<Object[]> questionCodes3 = q3.getResultList();
				//
				// questionCodes.addAll(questionCodes2);
				// questionCodes.addAll(questionCodes3);

				// List<String> qqs = em.createQuery(
				// "SELECT qq.code FROM QuestionQuestion qq where qq.pk.sourceCode IS NULL and
				// qq.pk.source.realm=:realm")
				// .setParameter("realm", realm).getResultList();
				String json = JsonUtils.toJson(questionCodes);

				return Response.status(200).entity(json).build();
			} catch (Exception e) {
				log.error("Error in fetching forms for realm " + realm + " " + e.getLocalizedMessage());
				return Response.status(401).entity("Query did not work.").build();
			}

		}
		return Response.status(401).entity("You need to be a test.").build();

	}
}
