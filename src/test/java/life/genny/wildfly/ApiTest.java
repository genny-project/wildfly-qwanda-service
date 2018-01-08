package life.genny.wildfly;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;
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
import org.json.JSONObject;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.constants.ServiceUrlConstants;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import life.genny.qwanda.DateTimeDeserializer;
import life.genny.qwandautils.QwandaUtils;

public class ApiTest {
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	GsonBuilder gsonBuilder = new GsonBuilder();
	Gson gson = null;

	@Test
	public void asks2Test() {
		// http://localhost:8280/qwanda/baseentitys/PER_USER1/asks2/QUE_OFFER_DETAILS_GRP/OFR_OFFER1

		if (System.getenv("GENNY_DEV") != null) { // only run when in eclipse dev mode

			String hostip = System.getenv("HOSTIP");
			if (hostip == null)
				hostip = "localhost";

			gsonBuilder.registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer());
			gson = gsonBuilder.create();

			String qwandaurl = System.getenv("QWANDA_URL");
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
}
