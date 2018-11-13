package life.genny.qwanda.observers;

import java.io.IOException;
import java.lang.invoke.MethodHandles;

import javax.ejb.Asynchronous;
import javax.enterprise.event.Observes;

import org.apache.logging.log4j.Logger;

import life.genny.qwanda.message.QEventAttributeValueChangeMessage;
import life.genny.qwandautils.JsonUtils;
import life.genny.qwandautils.QwandaUtils;

public class InsertObservers {
  /**
   * Stores logger object.
   */
  protected static final Logger log = org.apache.logging.log4j.LogManager
      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

  String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");

  @Asynchronous
  public void attributeValueChangeEvent(@Observes final QEventAttributeValueChangeMessage event) {
    // Send a vertx message broadcasting an attribute value Change
    log.info("ATTRIBUTE CHANGE EVENT!" + event.getAnswer().getTargetCode() + ":" + event.getAnswer().getValue()
        + " -> was " + event.getOldValue());
    try {
      QwandaUtils.apiPostEntity(bridgeApi, JsonUtils.toJson(event), event.getToken());
    } catch (final IOException e) {

      log.error("Error in posting to Vertx bridge:" + event.getAnswer().getTargetCode() + ":"
          + event.getAnswer().getValue() + " -> was " + event.getOldValue());
    }

  }

}
