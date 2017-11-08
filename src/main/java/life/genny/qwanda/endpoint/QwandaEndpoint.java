package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.Logger;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.keycloak.KeycloakPrincipal;
import org.keycloak.KeycloakSecurityContext;
import org.mortbay.log.Log;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import life.genny.qwanda.Answer;
import life.genny.qwanda.AnswerLink;
import life.genny.qwanda.Ask;
import life.genny.qwanda.GPS;
import life.genny.qwanda.Question;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.model.Setup;
import life.genny.qwanda.rule.Rule;
import life.genny.qwanda.service.BaseEntityService;

/**
 * JAX-RS endpoint
 *
 * @author Adam Crow
 */

@Path("/qwanda")
@Api(value = "/qwanda", description = "Qwanda API", tags = "qwanda")
@Produces(MediaType.APPLICATION_JSON)

@Stateless
// @RequestScoped
@Transactional
public class QwandaEndpoint {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Context
	SecurityContext sc;

	@Inject
	private BaseEntityService service;

	public static class HibernateLazyInitializerSerializer extends JsonSerializer<JavassistLazyInitializer> {

		@Override
		public void serialize(final JavassistLazyInitializer initializer, final JsonGenerator jsonGenerator,
				final SerializerProvider serializerProvider) throws IOException, JsonProcessingException {
			jsonGenerator.writeNull();
		}
	}

	@POST
	@Consumes("application/json")
	@Path("/rules")
	public Response create(final Rule entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/attributes")

	public Response create(final Attribute entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/questions")
	public Response create(final Question entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/asks")
	public Response create(final Ask entity) {

		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/baseentitys/{sourceCode}/questions/{questionCode}/{targetCode}")
	public Response createAsks(@PathParam("sourceCode") final String sourceCode,
			@PathParam("questionCode") final String questionCode, @PathParam("targetCode") final String targetCode,
			@Context final UriInfo uriInfo) {

		// Fetch the associated BaseEntitys and Question
		BaseEntity beSource = service.findBaseEntityByCode(sourceCode);
		BaseEntity beTarget = service.findBaseEntityByCode(targetCode);
		Question question = service.findQuestionByCode(questionCode);
		Ask newAsk = new Ask(question, beSource.getCode(), beTarget.getCode());

		Log.info("Creating new Ask " + beSource.getCode() + ":" + beTarget.getCode() + ":" + question.getCode());

		service.insert(newAsk);
		return Response.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(newAsk)).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/gpss")
	public Response create(final GPS entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/answers")
	public Response create(final Answer entity) {

		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/baseentitys")
	public Response create(final BaseEntity entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Path("/baseentitys")
	@Consumes("application/json")
	public Response create(@FormParam("name") final String name, @FormParam("uniqueCode") final String uniqueCode) {
		final BaseEntity entity = new BaseEntity(uniqueCode, name);
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@GET
	@Path("/rules/{id}")
	public Response fetchRuleById(@PathParam("id") final Long id) {
		final Rule entity = service.findRuleById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/baseentitys/{sourceCode}")
	public Response fetchBaseEntityByCode(@Context final ServletContext servletContext,
			@PathParam("sourceCode") final String code) {

		final BaseEntity entity = service.findBaseEntityByCode(code);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/questions/{id}")
	public Response fetchQuestionById(@PathParam("id") final Long id) {
		final Question entity = service.findQuestionById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/questions")
	public Response fetchQuestions() {
		final List<Question> entitys = service.findQuestions();
		return Response.status(200).entity(entitys).build();
	}

	@GET
	@Path("/questions/<questionCode}")
	public Response fetchQuestions(@PathParam("questionCode") final String questionCode) {
		final List<Question> entitys = service.findQuestions();
		return Response.status(200).entity(entitys).build();
	}

	@GET
	@Path("/rules")
	public Response fetchRules() {
		final List<Rule> entitys = service.findRules();

		System.out.println(entitys);
		return Response.status(200).entity(entitys).build();
	}

	@GET
	@Path("/asks")
	// @RolesAllowed("admin")
	public Response fetchAsks() {
		final List<Ask> entitys = service.findAsksWithQuestions();
		return Response.status(200).entity(entitys).build();
	}

	@GET
	@Path("/asksmsg")
	public Response fetchAsksMsg() {
		final List<Ask> entitys = service.findAsks();
		final QDataAskMessage qasks = new QDataAskMessage(entitys.toArray(new Ask[0]));
		System.out.println(qasks);
		return Response.status(200).entity(qasks).build();
	}

	@GET
	@Path("/attributes/{id}")
	public Response fetchAttributeById(@PathParam("id") final Long id) {
		final Attribute entity = service.findAttributeById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/asks/{id}")
	public Response fetchAskById(@PathParam("id") final Long id) {
		final Ask entity = service.findAskById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/answers/{id}")
	public Response fetchAnswerById(@PathParam("id") final Long id) {
		final Answer entity = service.findAnswerById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/contexts/{id}")
	public Response fetchContextById(@PathParam("id") final Long id) {
		final life.genny.qwanda.Context entity = service.findContextById(id);
		return Response.status(200).entity(entity).build();
	}

	@GET
	@Path("/baseentitys/{sourceCode}/attributes")
	@ApiOperation(value = "attributes", notes = "BaseEntity Attributes")
	@Produces(MediaType.APPLICATION_JSON)
	public List<EntityAttribute> fetchAttributesByBaseEntityCode(@PathParam("sourceCode") final String code) {
		final List<EntityAttribute> entityAttributes = service.findAttributesByBaseEntityCode(code);
		return entityAttributes;
	}

	@GET
	@Path("/baseentitys/{id}/gps")
	@ApiOperation(value = "gps", notes = "Target BaseEntity GPS")
	@Produces(MediaType.APPLICATION_JSON)
	public List<GPS> fetchGPSByTargetBaseEntityId(@PathParam("id") final Long id) {
		final List<GPS> items = service.findGPSByTargetBaseEntityId(id);
		return items;
	}

	@GET
	@Path("/baseentitys/{code}/asks/source")
	@ApiOperation(value = "asks", notes = "Source BaseEntity Asks")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Ask> fetchAsksBySourceBaseEntityCode(@PathParam("code") final String code) {
		final List<Ask> items = service.findAsksBySourceBaseEntityCode(code);
		return items;
	}

	@GET
	@Path("/baseentitys/{code}/asks/target")
	@ApiOperation(value = "asks", notes = "Target BaseEntity Asks")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Ask> fetchAsksByTargetBaseEntityCode(@PathParam("code") final String code) {
		final List<Ask> items = service.findAsksByTargetBaseEntityCode(code);
		return items;
	}

	@GET
	@Path("/baseentitys/{id}/asks/target")
	@ApiOperation(value = "asks", notes = "BaseEntity Asks about Targets")
	@Produces(MediaType.APPLICATION_JSON)
	public List<Ask> fetchAsksByTargetBaseEntityId(@PathParam("id") final Long id) {
		final List<Ask> items = service.findAsksBySourceBaseEntityId(id);
		return items;
	}

	@GET
	@Path("/baseentitys/{targetCode}/answers")
	@ApiOperation(value = "answers", notes = "BaseEntity AnswerLinks")
	@Produces(MediaType.APPLICATION_JSON)
	public List<AnswerLink> fetchAnswersByTargetBaseEntityCode(@PathParam("targetCode") final String targetCode) {
		final List<AnswerLink> items = service.findAnswersByTargetBaseEntityCode(targetCode);
		return items;
	}

	@GET
	@Path("/answers")
	@ApiOperation(value = "answers", notes = "AnswerLinks")
	@Produces(MediaType.APPLICATION_JSON)
	public Response fetchAnswerLinks() {
		final List<AnswerLink> items = service.findAnswerLinks();
		return Response.status(200).entity(items).build();
	}

	// @GET
	// @Path("/logout")
	// @ApiOperation(value = "Logout", notes = "Logout", response = String.class)
	// @Produces(MediaType.APPLICATION_JSON)
	// public Response logout(@Context final javax.servlet.http.HttpServletRequest
	// request) {
	// try {
	// request.logout();
	// } catch (final ServletException e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// return Response.status(200).entity("Logged Out").build();
	// }

	@GET
	@Path("/init")
	@Produces("application/json")
	public Response init() {
		if (sc != null) {
			if (sc.getUserPrincipal() != null) {
				if (sc.getUserPrincipal() instanceof KeycloakPrincipal) {
					final KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) sc
							.getUserPrincipal();

					service.init(kp.getKeycloakSecurityContext());
				}
			}
		}
		return Response.status(200).entity("Initialised").build();
	}

	@GET
	@Path("/baseentitys")
	@Produces("application/json")
	public Response getBaseEntitys(@Context final UriInfo uriInfo) {
		Integer pageStart = 0;
		Integer pageSize = 10; // default
		boolean includeAttributes = false;
		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		final String includeAttributesStr = params.getFirst("attributes");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}
		if (includeAttributesStr != null) {
			includeAttributes = true;
			qparams.remove("attributes");
		}
		final List<BaseEntity> targets = service.findBaseEntitysByAttributeValues(qparams, includeAttributes, pageStart,
				pageSize);
		if (!includeAttributes) {
			targets.parallelStream().forEach(t -> t.setBaseEntityAttributes(null));
		}
		return Response.status(200).entity(targets).build();
	}

	@GET
	@Path("/setup")
	@Produces("application/json")
	public Response getSetup() {
		Setup setup = new Setup();
		setup.setLayout("error-layout1");

		// this will set the user id as userName
		if (sc != null) {
			if (sc.getUserPrincipal() != null) {
				sc.getUserPrincipal().getName();

				if (sc.getUserPrincipal() instanceof KeycloakPrincipal) {
					final KeycloakPrincipal<KeycloakSecurityContext> kp = (KeycloakPrincipal<KeycloakSecurityContext>) sc
							.getUserPrincipal();

					// this is how to get the real userName (or rather the login
					// name)

					System.out.println("kc context:" + kp.getKeycloakSecurityContext());
					setup = service.setup(kp.getKeycloakSecurityContext());
				}
			}
		}
		return Response.status(200).entity(setup).build();

	}

	@GET
	@Path("/baseentitys/{sourceCode}/linkcodes/{linkCode}")
	@Produces("application/json")
	public Response getTargets(@PathParam("sourceCode") final String sourceCode,
			@DefaultValue("LNK_CORE") @PathParam("linkCode") final String linkCode, @Context final UriInfo uriInfo) {
		Integer pageStart = 0;
		Integer pageSize = 10; // default
		boolean includeAttributes = false;

		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		final String includeAttributesStr = params.getFirst("attributes");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}
		if (includeAttributesStr != null) {
			includeAttributes = true;
			qparams.remove("attributes");
		}

		final List<BaseEntity> targets = service.findChildrenByAttributeLink(sourceCode, linkCode, includeAttributes,
				pageStart, pageSize, 1, qparams);

		// remove the attributes
		if (!includeAttributes) {
			targets.parallelStream().forEach(t -> t.setBaseEntityAttributes(null));
		}

		Long total = service.findChildrenByAttributeLinkCount(sourceCode, linkCode, qparams);

		BaseEntity[] beArr = new BaseEntity[targets.size()];
		beArr = targets.toArray(beArr);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, sourceCode, linkCode);
		msg.setTotal(total);
		return Response.status(200).entity(msg).build();
	}

	@GET
	@Path("/baseentitys/{sourceCode}/linkcodes/{linkCode}/attributes")
	@Produces("application/json")
	public Response getTargetsWithAttributes(@PathParam("sourceCode") final String sourceCode,
			@DefaultValue("LNK_CORE") @PathParam("linkCode") final String linkCode, @Context final UriInfo uriInfo) {
		Integer pageStart = 0;
		Integer pageSize = 10; // default
		Integer level = 1;

		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		final String levelStr = params.getFirst("level");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}
		if (levelStr != null) {
			level = Integer.decode(levelStr);
			// params.remove("level");
		}
		final List<BaseEntity> targets = service.findChildrenByAttributeLink(sourceCode, linkCode, true, pageStart,
				pageSize, level, qparams);

		for (final BaseEntity be : targets) {
			log.info("\n" + be.getCode() + " + attributes");
			be.getBaseEntityAttributes().stream().forEach(p -> System.out.println(p.getAttributeCode()));
		}

		Long total = service.findChildrenByAttributeLinkCount(sourceCode, linkCode, qparams);

		BaseEntity[] beArr = new BaseEntity[targets.size()];
		beArr = targets.toArray(beArr);
		QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, sourceCode, linkCode);
		msg.setTotal(total);
		return Response.status(200).entity(msg).build();
	}

	@GET
	@Path("/baseentitys/test2")
	@Produces("application/json")
	public Response findBaseEntitysByAttributeValues(@Context final UriInfo uriInfo) {
		Integer pageStart = 0;
		Integer pageSize = 10; // default
		MultivaluedMap<String, String> params = uriInfo.getQueryParameters();
		MultivaluedMap<String, String> qparams = new MultivaluedMapImpl<String, String>();
		qparams.putAll(params);

		final String pageStartStr = params.getFirst("pageStart");
		final String pageSizeStr = params.getFirst("pageSize");
		if (pageStartStr != null) {
			pageStart = Integer.decode(pageStartStr);
			qparams.remove("pageStart");
		}
		if (pageSizeStr != null) {
			pageSize = Integer.decode(pageSizeStr);
			qparams.remove("pageSize");
		}

		final List<BaseEntity> targets = service.findBaseEntitysByAttributeValues(qparams, true, pageStart, pageSize);

		BaseEntity[] beArr = new BaseEntity[targets.size()];
		beArr = targets.toArray(beArr);
		final Long total = service.findBaseEntitysByAttributeValuesCount(params);

		final QDataBaseEntityMessage msg = new QDataBaseEntityMessage(beArr, "", "", total);

		return Response.status(200).entity(msg).build();
	}

	@GET
	@Path("/baseentitys/importkeycloak")
	@Produces("application/json")
	// user MUST BE SUPERADMIN
	public Response importKeycloakUsers(@QueryParam("keycloakurl") final String keycloakUrl,
			@QueryParam("realm") final String realm, @QueryParam("username") final String username,
			@QueryParam("password") final String password, @QueryParam("clientid") final String clientId,
			@QueryParam("max") final Integer max,
			@DefaultValue("GRP_USERS") @QueryParam("parentgroups") final String parentGroupCodes) {

		Long usersAddedCount = service.importKeycloakUsers(keycloakUrl, realm, username, password, clientId, max,
				parentGroupCodes);
		return Response.status(200).entity(usersAddedCount).build();
	}

	@POST
	@Path("/baseentitys/uploadcsv")
	@Consumes("multipart/form-data")
	public Response uploadFile(final MultipartFormDataInput input) throws IOException {

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

}
