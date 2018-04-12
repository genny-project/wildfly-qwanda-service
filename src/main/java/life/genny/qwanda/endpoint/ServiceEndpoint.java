package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.apache.logging.log4j.Logger;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;

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
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.controller.Controller;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;
import life.genny.qwandautils.GPSUtils;
import life.genny.qwandautils.JsonUtils;

/**
 * JAX-RS endpoint
 *
 * @author Adam Crow
 */

@Path("/service")
@Api(value = "/service", description = "Qwanda Service API", tags = "qwandaservice")
@Produces(MediaType.APPLICATION_JSON)

@RequestScoped
public class ServiceEndpoint {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	Boolean devMode = "TRUE".equals(System.getenv("GENNYDEV"));

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

		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

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
	public Response uploadFile(final MultipartFormDataInput input) throws IOException {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

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
	@Path("/synchronize/{sheetId}/{tables}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response synchronizeSheets2DB(@PathParam("sheetId") final String sheetId,
			@PathParam("tables") final String tables) {
		String response = "Success";
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

			ctl.getProject(service, sheetId, tables);
		}
		return Response.status(200).entity(response).build();
	}

	@GET
	@Path("/synchronize/cache/attributes")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response synchronizeCache() {
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

			service.pushAttributes();
		}
		return Response.status(200).entity("ok").build();
	}

	@GET
	@Path("/synchronize/cache/baseentitys")
	@ApiOperation(value = "baseentitys", notes = "Sync all BaseEntitys")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response synchronizeCacheBEs() {

		List<BaseEntity> results = new ArrayList<BaseEntity>();
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

			results = em.createQuery("SELECT distinct be FROM BaseEntity be JOIN  be.baseEntityAttributes ea ")
					.getResultList();
			for (BaseEntity be : results) {
				service.writeToDDT(be);
			}
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
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

			results = service.readFromDDT(key);
		}
		return Response.status(200).entity(results).build();
	}
	
	@GET
	@Path("/answers/{sourceCode}/{targetCode}/{attributeCode}/{value}")
	@ApiOperation(value = "answer", notes = "quick answer")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response setAnswer(@PathParam("sourceCode") final String sourceCode,@PathParam("targetCode") final String targetCode, @PathParam("attributeCode") final String attributeCode, @PathParam("value") final String value) {
		BaseEntity result = null;
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

			Answer answer = new Answer(sourceCode, targetCode, attributeCode, value);
			Attribute attribute = service.findAttributeByCode(attributeCode);
			if (attribute == null) {
				log.error("Bad attribute supplied "+attributeCode);
			} else {
			answer.setAttribute(attribute);
			Answer[] answerArray = new Answer[1];
			answerArray[0] = answer;
		
			
			service.insert(answerArray);
			result = (BaseEntity) em.createQuery("SELECT be FROM BaseEntity be JOIN  be.baseEntityAttributes ea where be.code=:code")
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
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

			result = (BaseEntity) em.createQuery("SELECT be FROM BaseEntity be JOIN  be.baseEntityAttributes ea where be.code=:code")
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
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

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
			@PathParam("destination") final String destinationAddress,
			@PathParam("percentage") final Double percentage) {
		String json = null;
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

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
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {

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
		
		if (securityService.inRole("superadmin") || securityService.inRole("dev") || devMode) {
			service.removeBaseEntity(code);
				
			} else {
				returnMessage = "Cannot find BE "+code;
			}

	
		return Response.status(200).entity(returnMessage).build();
	}

	
}
