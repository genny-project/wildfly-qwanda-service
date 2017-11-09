package life.genny.qwanda.observers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.time.LocalDateTime;

import javax.enterprise.event.Observes;

import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import life.genny.qwanda.DateTimeDeserializer;
import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwandautils.QwandaUtils;

public class InsertObservers {
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

	GsonBuilder gsonBuilder = new GsonBuilder().registerTypeAdapter(LocalDateTime.class, new DateTimeDeserializer());
	String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");

	public void attributeValueChangeEvent(@Observes final QEventAttributeValueChangeMessage event) {
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

}
