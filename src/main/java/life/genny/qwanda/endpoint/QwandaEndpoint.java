package life.genny.qwanda.endpoint;

import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.servlet.ServletContext;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.logging.log4j.Logger;
import org.hibernate.proxy.pojo.javassist.JavassistLazyInitializer;
import org.jboss.resteasy.plugins.providers.multipart.InputPart;
import org.jboss.resteasy.plugins.providers.multipart.MultipartFormDataInput;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.json.JSONObject;
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
import life.genny.qwanda.Link;
import life.genny.qwanda.Question;
import life.genny.qwanda.attribute.Attribute;
import life.genny.qwanda.attribute.EntityAttribute;
import life.genny.qwanda.controller.Controller;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.entity.EntityEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwanda.message.QDataAskMessage;
import life.genny.qwanda.message.QDataBaseEntityMessage;
import life.genny.qwanda.rule.Rule;
import life.genny.qwanda.service.SecurityService;
import life.genny.qwanda.service.Service;
import life.genny.security.SecureResources;

/**
 * JAX-RS endpoint
 *
 * @author Adam Crow
 */

@Path("/qwanda")
@Api(value = "/qwanda", description = "Qwanda API", tags = "qwanda")
@Produces(MediaType.APPLICATION_JSON)

// @Stateless
@RequestScoped
public class QwandaEndpoint {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

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

	@POST
	@Consumes("application/json")
	@Path("/rules")
	@Transactional
	public Response create(final Rule entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/attributes")
	@Transactional
	public Response create(final Attribute entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/questions")
	@Transactional
	public Response create(final Question entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/asks")
	@Transactional
	public Response create(final Ask entity) {

		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	// TODO: should be POST
	@GET
	@Consumes("application/json")
	@Path("/baseentitys/{sourceCode}/asks/{questionCode}/{targetCode}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response createAsks(@PathParam("sourceCode") final String sourceCode,
			@PathParam("questionCode") final String questionCode, @PathParam("targetCode") final String targetCode,
			@Context final UriInfo uriInfo) {

		List<Ask> asks = service.createAsksByQuestionCode(questionCode, sourceCode, targetCode);
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));

		// return
		// Response.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(askMsgs)).build())
		// .build();
		return Response.status(200).entity(askMsgs).build();
	}

	@GET
	@Consumes("application/json")
	@Path("/baseentitys/{sourceCode}/asks2/{questionCode}/{targetCode}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response createAsks2(@PathParam("sourceCode") final String sourceCode,
			@PathParam("questionCode") final String questionCode, @PathParam("targetCode") final String targetCode,
			@Context final UriInfo uriInfo) {

		List<Ask> asks = service.createAsksByQuestionCode2(questionCode, sourceCode, targetCode);
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));

		// return
		// Response.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(askMsgs)).build())
		// .build();
		return Response.status(200).entity(askMsgs).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/gpss")
	@Transactional
	public Response create(final GPS entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/answers")
	@Transactional
	public Response create(final Answer entity) {

		if (entity.getAttribute() == null) {
			Attribute attribute = service.findAttributeByCode(entity.getAttributeCode());
			entity.setAttribute(attribute);
		}
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	@POST
	@Consumes("application/json")
	@Path("/baseentitys")
	@Transactional
	public Response create(final BaseEntity entity) {
		service.insert(entity);
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
				.build();
	}

	// @POST
	// @Path("/baseentitys")
	// @Consumes("application/json")
	// @Transactional
	// public Response create(@FormParam("name") final String name,
	// @FormParam("uniqueCode") final String uniqueCode) {
	// final BaseEntity entity = new BaseEntity(uniqueCode, name);
	// service.insert(entity);
	// return Response
	// .created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(entity.getId())).build())
	// .build();
	// }

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
	@Path("/questions/{questionCode}")
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

	// TODO: This should be deprecated
	@GET
	@Path("/asksmsg/{questionCode}")
	@Transactional
	public Response fetchAsksMsgByQuestionCode(@PathParam("questionCode") final String questionCode) {
		// work out who the sourceCode and targetCode
		BaseEntity user = service.getUser();
		List<Ask> asks = service.createAsksByQuestionCode(questionCode, user.getCode(), user.getCode());
		final QDataAskMessage askMsgs = new QDataAskMessage(asks.toArray(new Ask[0]));
		return Response.status(200).entity(askMsgs).build();
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
	@Transactional
	public List<AnswerLink> fetchAnswersByTargetBaseEntityCode(@PathParam("targetCode") final String targetCode) {
		final List<AnswerLink> items = service.findAnswersByTargetBaseEntityCode(targetCode);
		return items;
	}

	@GET
	@Path("/answers")
	@ApiOperation(value = "answers", notes = "AnswerLinks")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
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

	// @GET
	// @Path("/setup")
	// @Produces("application/json")
	// public Response getSetup() {
	// Setup setup = new Setup();
	// setup.setLayout("error-layout1");
	//
	// // this will set the user id as userName
	// if (sc != null) {
	// if (sc.getUserPrincipal() != null) {
	// sc.getUserPrincipal().getName();
	//
	// if (sc.getUserPrincipal() instanceof KeycloakPrincipal) {
	// final KeycloakPrincipal<KeycloakSecurityContext> kp =
	// (KeycloakPrincipal<KeycloakSecurityContext>) sc.getUserPrincipal();
	//
	// // this is how to get the real userName (or rather the login
	// // name)
	//
	// System.out.println("kc context:" + kp.getKeycloakSecurityContext());
	// setup = service.setup(kp.getKeycloakSecurityContext());
	// }
	// }
	// }
	// return Response.status(200).entity(setup).build();
	//
	// }

	@GET
	@Path("/baseentitys/{sourceCode}/linkcodes/{linkCode}")
	@Produces("application/json")
	public Response getTargets(@PathParam("sourceCode") final String sourceCode,
			@DefaultValue("LNK_CORE") @PathParam("linkCode") final String linkCode, @Context final UriInfo uriInfo) {
		log.info("Entering GET TARGETS /baseentitys/{sourceCode}/linkcodes/{linkCode}");
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

		log.info("Entering GET TARGETSCOUNT/baseentitys/{sourceCode}/linkcodes/{linkCode}");
		Long total = -1L;

		try {
			total = service.findChildrenByAttributeLinkCount(sourceCode, linkCode, qparams);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			total = -1L;
		}

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
	@Transactional
	// user MUST BE SUPERADMIN
	public Response importKeycloakUsers(@QueryParam("keycloakurl") final String keycloakUrl,
			@QueryParam("realm") final String realm, @QueryParam("username") final String username,
			@QueryParam("password") final String password, @QueryParam("clientid") final String clientId,
			@QueryParam("max") final Integer max,
			@DefaultValue("GRP_USERS") @QueryParam("parentgroups") final String parentGroupCodes) {
		log.error("IMPORT KEYCLOAK DISABLED IN CODE");
		Long usersAddedCount = 0L; // service.importKeycloakUsers(keycloakUrl, realm, username,
									// password, clientId,
									// max, parentGroupCodes);
		return Response.status(200).entity(usersAddedCount).build();
	}

	@POST
	@Path("/baseentitys/uploadcsv")
	@Consumes("multipart/form-data")
	@Transactional
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

	@POST
	@Consumes("application/json")
	@Path("/entityentitys")
	@Produces("application/json")
	@Transactional
	public Response addLink(final Link ee) {

		// BaseEntity parent = service.findBaseEntityByCode(ee.getParentCode());
		// BaseEntity target = service.findBaseEntityByCode(ee.getTargetCode());
		// AttributeLink link = service.findAttributeLinkByCode(ee.getLinkCode());

		// ee.setLinkAttribute(link);
		// ee.setSource(parent);
		// ee.setTarget(target);
		// ee.setValue("TEST");
		Log.info("Creating new Link " + ee.getSourceCode() + ":" + ee.getTargetCode() + ":" + ee.getAttributeCode()
				+ ":" + ee.getLinkValue());

		EntityEntity newEntityEntity = null;

		try {
			newEntityEntity = service.addLink(ee.getSourceCode(), ee.getTargetCode(), ee.getAttributeCode(),
					ee.getLinkValue(), 1.0);
		} catch (IllegalArgumentException | BadDataException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(newEntityEntity)).build())
				.build();
	}

	@DELETE
	@Consumes("application/json")
	@Path("/entityentitys")
	@Produces("application/json")
	@Transactional
	public Response removeLink(final Link ee) {

		Log.info("Removing Link " + ee.getSourceCode() + ":" + ee.getTargetCode() + ":" + ee.getAttributeCode());

		service.removeLink(ee.getSourceCode(), ee.getTargetCode(), ee.getAttributeCode());
		return Response.created(UriBuilder.fromResource(QwandaEndpoint.class).build()).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/baseentitys/move/{targetCode}")
	@Produces("application/json")
	@Transactional
	public Response moveLink(@PathParam("targetCode") final String targetCode, final Link ee) {

		Log.info("moving Link " + ee.getSourceCode() + ":" + ee.getTargetCode() + ":" + ee.getAttributeCode()
				+ " to new Parent " + targetCode);
		Link newEntityEntity = service.moveLink(ee.getSourceCode(), ee.getTargetCode(), ee.getAttributeCode(),
				targetCode);
		// TODO: This is a terrible hack.but logically will work
		newEntityEntity.setAttributeCode(ee.getAttributeCode());
		newEntityEntity.setSourceCode(targetCode);
		newEntityEntity.setTargetCode(ee.getTargetCode());
		return Response
				.created(UriBuilder.fromResource(QwandaEndpoint.class).path(String.valueOf(newEntityEntity)).build())
				.build();
	}

	@GET
	@Path("/entityentitys/{targetCode}/linkcodes/{linkCode}")
	@ApiOperation(value = "baseentitys/{targetCode}/links", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchLinks(@PathParam("targetCode") final String targetCode,
			@PathParam("linkCode") final String linkCode) {
		final List<Link> items = service.findLinks(targetCode, linkCode);

		return Response.status(200).entity(items).build();
	}

	@GET
	@Path("/entityentitys/{sourceCode}/linkcodes/{linkCode}/children")
	@ApiOperation(value = "baseentitys/{sourceCode}/linkcodes/{linkCode}/children", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchChildLinks(@PathParam("sourceCode") final String sourceCode,
			@PathParam("linkCode") final String linkCode) {
		final List<Link> items = service.findChildLinks(sourceCode, linkCode);

		return Response.status(200).entity(items).build();
	}

	@GET
	@Path("/realms/sync")
	@ApiOperation(value = "syncrealms", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response syncrealms() {
		System.out.println("Sync Keycloak Realms");
		String keycloakRealms = SecureResources.reload();
		return Response.status(200).entity(keycloakRealms).build();
	}

	@GET
	@Path("/realms")
	@ApiOperation(value = "syncrealms", notes = "Links")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response fetchrealms() {
		System.out.println("Fetch Keycloak Realms");
		String keycloakRealms = SecureResources.fetchRealms();
		return Response.status(200).entity(keycloakRealms).build();
	}

	@POST
	@Consumes("application/json")
	@Path("/realms")
	@Transactional
	public Response createRealm(final String entity) {
		System.out.println("Add Keycloak Realm");

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

		Log.info("Removing Realm " + key);

		SecureResources.removeRealm(key);
		return Response.created(UriBuilder.fromResource(QwandaEndpoint.class).build()).build();
	}

	Controller ctl = new Controller();

	@GET
	@Path("/synchronize/{sheetId}/{tables}")
	@Produces(MediaType.APPLICATION_JSON)
	@Transactional
	public Response synchronizeSheets2DB(@PathParam("sheetId") final String sheetId,
			@PathParam("tables") final String tables) {
		String response = "Success";
		ctl.getProject(service, sheetId, tables);
		return Response.status(200).entity(response).build();
	}

}
