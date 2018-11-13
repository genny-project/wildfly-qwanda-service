package life.genny.wildfly;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.logging.log4j.Logger;
import org.javamoney.moneta.Money;
import org.json.JSONObject;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;

import life.genny.qwanda.Link;
import life.genny.qwanda.MoneyDeserializer;
import life.genny.qwanda.attribute.AttributeInteger;
import life.genny.qwanda.attribute.AttributeText;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.exception.BadDataException;
import life.genny.qwandautils.GennySettings;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;

public class ApiTest {
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	@Test
	public void searchTest() {
		if (GennySettings.DEV_MODE) { // only run when in eclipse dev mode
			String hostip = System.getenv("HOSTIP");
			if (hostip == null)
				hostip = "localhost";

			String qwandaurl = GennySettings.QWANDA_SERVICE_URL;
			if (qwandaurl == null) {
				qwandaurl = "http://" + hostip + ":8280";
			}

			String keycloakurl = System.getenv("KEYCLOAK_URL");
			if (keycloakurl == null) {
				keycloakurl = "http://" + hostip + ":8180";
			}

			String secret = System.getenv("SECRET");
			if (secret == null) {
				secret = "056b73c1-7078-411d-80ec-87d41c55c3b4";
			}
			String accessTokenResponse = null;
			try {
				accessTokenResponse = getAccessToken(keycloakurl, "genny", "genny", secret, "user1", "password1");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			JSONObject json = new JSONObject(accessTokenResponse);
			String token = json.getString("access_token");

			System.out.println("Token =" + token);
			BaseEntity searchBE = new BaseEntity("SER_TEST_SEARCH", "Search test");
			try {
				// searchBE.setValue(new AttributeText("SCH_STAKEHOLDER_CODE",
				// "Stakeholder"),"PER_USER1");
				searchBE.setValue(new AttributeInteger("SCH_PAGE_START", "PageStart"), 0);
				searchBE.setValue(new AttributeInteger("SCH_PAGE_SIZE", "PageSize"), 10);

				// Set some Filter attributes
				// searchBE.setValue(new AttributeText("QRY_PRI_FIRST_NAME", "First
				// Name"),"Bob");

				// searchBE.setValue(new AttributeDate("QRY_PRI_DOB", "DOB"), LocalDate.of(2018,
				// 2, 20));

				// searchBE.setValue(new AttributeDate("PRI_DOB", "DOB"), LocalDate.of(2018, 2,
				// 20));
				// searchBE.setValue(new AttributeDate("PRI_DOB", "DOB"), null, 2.0);
				// searchBE.setValue(new AttributeText("SRT_PRI_DOB", "DOB"), "ASC", 0.8);
				searchBE.setValue(new AttributeText("SRT_PRI_FIRSTNAME", "FIRSTNAME"), "DESC", 1.0); // higher priority
																										// sorting
				searchBE.setValue(new AttributeText("SRT_PRI_LASTNAME", "LASTNAME"), "DESC", 2.0); // higher priority
				// sorting

				searchBE.setValue(new AttributeText("PRI_FIRSTNAME", "First name"), null, 1.0); // return this
																								// column with
																								// this header
				// searchBE.setValue(new AttributeText("PRI_DOB", "DOB"), "Birthday", 2.0); //
				// return this column with this
				// // header
				searchBE.setValue(new AttributeText("PRI_LASTNAME", "LastName"), null, 1.5); // return this column

				String results = QwandaUtils.apiPostEntity(qwandaurl + "/qwanda/baseentitys/search",
						JsonUtils.toJson(searchBE), token);
				System.out.println("Results=" + results);

			} catch (BadDataException e) {
				log.error("Bad Data Exception");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Test
	public void asks2Test() {
		// http://localhost:8280/qwanda/baseentitys/PER_USER1/asks2/QUE_OFFER_DETAILS_GRP/OFR_OFFER1

		if (GennySettings.DEV_MODE) { // only run when in eclipse dev mode

			String hostip = System.getenv("HOSTIP");
			if (hostip == null)
				hostip = "localhost";

			String qwandaurl = GennySettings.QWANDA_SERVICE_URL;
			if (qwandaurl == null) {
				qwandaurl = "http://" + hostip + ":8280";
			}

			String keycloakurl = System.getenv("KEYCLOAK_URL");
			if (keycloakurl == null) {
				keycloakurl = "http://" + hostip + ":8180";
			}

			String secret = System.getenv("SECRET");
			if (secret == null) {
				secret = "056b73c1-7078-411d-80ec-87d41c55c3b4";
			}
			String accessTokenResponse = null;
			try {
				accessTokenResponse = getAccessToken(keycloakurl, "genny", "genny", secret, "user1", "password1");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			JSONObject json = new JSONObject(accessTokenResponse);
			String token = json.getString("access_token");
			try {
				String ret = QwandaUtils.apiGet(
						qwandaurl + "/qwanda/baseentitys/PER_USER1/asks2/QUE_OFFER_DETAILS_GRP/PER_USER1", token);
				System.out.println(ret);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			log.info("GENNY_DEV not enabled for API testing");
		}
	}

	String getAccessToken(final String keycloakUrl, final String realm, final String clientId, final String secret,
			final String username, final String password) throws IOException {

		HttpClient httpClient = new DefaultHttpClient();

		try {
			HttpPost post = new HttpPost(KeycloakUriBuilder.fromUri(keycloakUrl + "/auth")
					.path(ServiceUrlConstants.TOKEN_PATH).build(realm));
			// System.out.println("url token post=" + keycloakUrl + "/auth" + ",tokenpath="
			// + ServiceUrlConstants.TOKEN_PATH + ":realm=" + realm + ":clientid=" +
			// clientId + ":secret" + secret
			// + ":un:" + username + "pw:" + password);
			// ;
			post.addHeader("Content-Type", "application/x-www-form-urlencoded");

			List<NameValuePair> formParams = new ArrayList<NameValuePair>();
			formParams.add(new BasicNameValuePair("username", username));
			formParams.add(new BasicNameValuePair("password", password));
			formParams.add(new BasicNameValuePair(OAuth2Constants.GRANT_TYPE, "password"));
			formParams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_ID, clientId));
			formParams.add(new BasicNameValuePair(OAuth2Constants.CLIENT_SECRET, secret));
			UrlEncodedFormEntity form = new UrlEncodedFormEntity(formParams, "UTF-8");

			post.setEntity(form);

			HttpResponse response = httpClient.execute(post);

			int statusCode = response.getStatusLine().getStatusCode();
			HttpEntity entity = response.getEntity();
			String content = null;
			if (statusCode != 200) {
				content = getContent(entity);
				throw new IOException("" + statusCode);
			}
			if (entity == null) {
				throw new IOException("Null Entity");
			} else {
				content = getContent(entity);
			}
			return content;
		} finally {
			httpClient.getConnectionManager().shutdown();
		}
	}

	public static String getContent(final HttpEntity httpEntity) throws IOException {
		if (httpEntity == null)
			return null;
		final InputStream is = httpEntity.getContent();
		try {
			final ByteArrayOutputStream os = new ByteArrayOutputStream();
			int c;
			while ((c = is.read()) != -1) {
				os.write(c);
			}
			final byte[] bytes = os.toByteArray();
			final String data = new String(bytes);
			return data;
		} finally {
			try {
				is.close();
			} catch (final IOException ignored) {

			}
		}

	}

	@Test
	public void linkTest() {
		if (GennySettings.DEV_MODE) { // only run when in eclipse dev mode

			String hostip = System.getenv("HOSTIP");
			if (hostip == null)
				hostip = "localhost";

			final Gson gson = new GsonBuilder().registerTypeAdapter(Money.class, new MoneyDeserializer())
					.registerTypeAdapter(LocalDateTime.class, new JsonDeserializer<LocalDateTime>() {
						@Override
						public LocalDateTime deserialize(final JsonElement json, final Type type,
								final JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
							return ZonedDateTime.parse(json.getAsJsonPrimitive().getAsString()).toLocalDateTime();
						}

						public JsonElement serialize(final LocalDateTime date, final Type typeOfSrc,
								final JsonSerializationContext context) {
							return new JsonPrimitive(date.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)); // "yyyy-mm-dd"
						}
					}).create();

			String qwandaurl = GennySettings.QWANDA_SERVICE_URL;
			if (qwandaurl == null) {
				qwandaurl = "http://" + hostip + ":8280";
			}

			String keycloakurl = System.getenv("KEYCLOAK_URL");
			if (keycloakurl == null) {
				keycloakurl = "http://" + hostip + ":8180";
			}

			String secret = System.getenv("SECRET");
			if (secret == null) {
				secret = "056b73c1-7078-411d-80ec-87d41c55c3b4";
			}
			String accessTokenResponse = null;
			try {
				accessTokenResponse = getAccessToken(keycloakurl, "genny", "genny", secret, "user1", "password1");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			JSONObject json = new JSONObject(accessTokenResponse);
			String token = json.getString("access_token");

			try {
				// Add a new Link
				Link newLink = new Link("GRP_USERS", "PER_USER1", "LNK_TEST", "1.2", new Double(3.14));
				String ret = QwandaUtils.apiPostEntity(qwandaurl + "/qwanda/entityentitys", gson.toJson(newLink),
						token);

				// EntityEntity ee = gson.fromJson(ret, EntityEntity.class);

				// log.info("EE returned for new link is " + ee);

				// Update Link
				Link updatedLink = new Link("GRP_USERS", "PER_USER1", "LNK_TEST", "1.3", new Double(6.14));
				updatedLink.setChildColor("BLACK");
				ret = QwandaUtils.apiPutEntity(qwandaurl + "/qwanda/entityentitys", gson.toJson(updatedLink), token);
				log.info("ret from link update is " + ret);
				// ee = gson.fromJson(ret, EntityEntity.class);

				// log.info("EE returned for updated link is " + ee);

				//QwandaUtils.apiDelete(qwandaurl + "/qwanda/entityentitys", gson.toJson(updatedLink), token);

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		} else {
			log.info("GENNY_DEV not enabled for API testing");
		}

		// try {
		// EntityEntity ee = service.addLink(testGroup.getCode(), user1.getCode(),
		// "LNK_TEST", new Double(3.14), 1.2);
		//
		// assertEquals(ee.getPk().getAttribute().getCode(),"LNK_TEST");
		//
		// // fetch link
		// EntityEntity newEntity = service.findEntityEntity(testGroup.getCode(),
		// user1.getCode(), "LNK_TEST");
		// assertEquals(newEntity.getPk().getAttribute().getCode(),"LNK_TEST");
		// assertEquals(newEntity.getPk().getSource().getCode(),testGroup.getCode());
		// assertEquals(newEntity.getPk().getTargetCode(),user1.getCode());
		//
		// final MultivaluedMap<String, String> params = new MultivaluedMapImpl<String,
		// String>();
		// // params.add("pageStart", "0");
		// // params.add("pageSize", "2");
		//
		//
		// List<BaseEntity> baseEntitys =
		// service.findChildrenByAttributeLink(testGroup.getCode(), "LNK_TEST", false,
		// 0, 10, 2, params);
		// List<BaseEntity> baseEntitys2 =
		// service.findChildrenByAttributeLink(testGroup2.getCode(), "LNK_TEST", false,
		// 0, 10, 2, params);
		// // Check baseEntitys has testGroup and no testGroup2
		// assertEquals(baseEntitys.contains(user1),true);
		// assertEquals(baseEntitys2.contains(user1),false);
		//
		//
		// // Move link!
		//
		// service.moveLink(testGroup.getCode(), user1.getCode(), "LNK_TEST",
		// testGroup2.getCode());
		// List<BaseEntity> baseEntitysA =
		// service.findChildrenByAttributeLink(testGroup.getCode(), "LNK_TEST", false,
		// 0, 10, 2, params);
		// List<BaseEntity> baseEntitys2A =
		// service.findChildrenByAttributeLink(testGroup2.getCode(), "LNK_TEST", false,
		// 0, 10, 2, params);
		// // Check baseEntitys has testGroup and no testGroup2
		// assertEquals(baseEntitysA.contains(user1),false);
		// assertEquals(baseEntitys2A.contains(user1),true);
		//
		// // now fetch all the links for a target
		// List<Link> links = service.findLinks(user1.getCode(), "LNK_TEST");
		// Integer linkCount = links.size();
		// assertEquals(linkCount==1,true);
		// assertEquals(links.get(0).getSourceCode().equals(testGroup2.getCode()),true);
		// // check it moved
		// } catch (IllegalArgumentException | BadDataException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// getEm().getTransaction().commit(
	}

}
