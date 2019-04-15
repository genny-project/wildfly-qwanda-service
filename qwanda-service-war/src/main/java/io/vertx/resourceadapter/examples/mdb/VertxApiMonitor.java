package io.vertx.resourceadapter.examples.mdb;

import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.inject.Inject;

import org.apache.logging.log4j.Logger;

import io.vertx.core.eventbus.Message;
import io.vertx.core.json.JsonObject;
import io.vertx.resourceadapter.inflow.VertxListener;




/**
 * Message-Driven Bean implementation class for: VertxApiMonitor
 */
//@MessageDriven(name = "VertxApiMonitor", messageListenerInterface = VertxListener.class)
@MessageDriven(name = "VertxApiMonitor", messageListenerInterface = VertxListener.class, activationConfig = { @ActivationConfigProperty(propertyName = "address", propertyValue = "api"), })

public class VertxApiMonitor implements VertxListener {
	

@Inject
EventBusBean eventBus;


  final static String st = System.getenv("MYIP");
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
	static Map<String, Object> decodedToken = null;
	static Set<String> userRoles = null;

	


	static String token;


  /**
   * Default constructor.
   */
  public VertxApiMonitor() {
    log.info("VertxApiMonitor started.");
  }

  @Override
  public <T> void onMessage(Message<T> message) {
	  final JsonObject payload = new JsonObject(message.body().toString());
    log.info("Got an api message from Vert.x: " + payload);


  }

  


}
