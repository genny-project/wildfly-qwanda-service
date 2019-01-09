package io.vertx.resourceadapter.examples.mdb;


import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import io.vertx.resourceadapter.*;
import life.genny.channel.Producer;
import life.genny.eventbus.EventBusInterface;
import life.genny.qwanda.entity.BaseEntity;
import javax.enterprise.context.ApplicationScoped;
import io.vertx.core.json.JsonObject;
import life.genny.qwandautils.QwandaUtils;
import java.lang.invoke.MethodHandles;
import org.apache.logging.log4j.Logger;

@ApplicationScoped
public class EventBusBean implements EventBusInterface {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
	String bridgeApi = System.getenv("REACT_APP_VERTX_SERVICE_API");

	public void write(final String channel, final Object msg) throws NamingException 
	{
		String json = (String)msg;
		JsonObject event = new JsonObject(json);
		
		if (System.getenv("FORCE_EVENTBUS_USE_API")!=null) {
			try {

				QwandaUtils.apiPostEntity(bridgeApi, json, event.getString("token"));
			} catch (Exception e) {
				log.error("Error in posting message to bridge eventbus:" + event);
			}
			
		} else {

		   javax.naming.InitialContext ctx = null;
		    io.vertx.resourceadapter.VertxConnection conn = null;
		    try {
		      ctx = new javax.naming.InitialContext();
		      io.vertx.resourceadapter.VertxConnectionFactory connFactory =
		          (io.vertx.resourceadapter.VertxConnectionFactory) ctx
		              .lookup("java:/eis/VertxConnectionFactory");
		      conn = connFactory.getVertxConnection();
		      conn.vertxEventBus().publish(channel, json);
		    } catch (Exception e) {
		      e.printStackTrace();
		    } finally {
		      if (ctx != null) {
		        ctx.close();
		      }
		      if (conn != null) {
		    	  try {
		        conn.close();
		    	  } catch (ResourceException e) {
		    		  e.printStackTrace();
		    	  }
		      }
		    }
		}
	}
  

	public void send(final String channel, final Object msg) throws NamingException 
	{
		String json = (String)msg;
		JsonObject event = new JsonObject(json);
		if ((System.getenv("FORCE_EVENTBUS_USE_API")!=null)&&(System.getenv("FORCE_EVENTBUS_USE_API").equalsIgnoreCase("TRUE"))) {
			try {
				QwandaUtils.apiPostEntity(bridgeApi, json, event.getString("token"));
			} catch (Exception e) {
				log.error("Error in posting message to bridge eventbus:" + event);
			}
			
			
		} else {
		   javax.naming.InitialContext ctx = null;
		    io.vertx.resourceadapter.VertxConnection conn = null;
		    try {
		      ctx = new javax.naming.InitialContext();
		      io.vertx.resourceadapter.VertxConnectionFactory connFactory =
		          (io.vertx.resourceadapter.VertxConnectionFactory) ctx
		              .lookup("java:/eis/VertxConnectionFactory");
		      log.info("Sending Vertx Bus Message:");
		      conn = connFactory.getVertxConnection();
		      conn.vertxEventBus().send(channel, json);
		      log.info("Sent Vertx Bus Message");
		    } catch (Exception e) {
		      e.printStackTrace();
		    } finally {
		      if (ctx != null) {
		        ctx.close();
		      }
		      if (conn != null) {
		    	  try {
		        conn.close();
		    	  } catch (ResourceException e) {
		    		  e.printStackTrace();
		    	  }
		      }
		    }
		}
	}
  // @PostConstruct
  public static void whatever() throws NamingException, ResourceException {
    javax.naming.InitialContext ctx = null;
    io.vertx.resourceadapter.VertxConnection conn = null;
    try {
      ctx = new javax.naming.InitialContext();
      io.vertx.resourceadapter.VertxConnectionFactory connFactory =
          (io.vertx.resourceadapter.VertxConnectionFactory) ctx
              .lookup("java:/eis/VertxConnectionFactory");
      conn = connFactory.getVertxConnection();
      conn.vertxEventBus().send("cmds", "Hello from JCA");
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (ctx != null) {
        ctx.close();
      }
      if (conn != null) {
        conn.close();
      }
    }
  }

	@Override
	public void publish(BaseEntity user, String channel, Object payload, final String[] filterAttributes) {
		try {
		// Actually Send ....
		switch (channel) {
		case "event":
		case "events":
			send("events",payload);
			break;
		case "data":
			write("data",payload);
			break;

		case "webdata":
			payload = EventBusInterface.privacyFilter(user, payload,filterAttributes);
			write("webdata",payload);
			break;
		case "cmds":
		case "webbcmds":
			payload = EventBusInterface.privacyFilter(user, payload,filterAttributes);
			write("webcmds",payload);
			break;
		case "services":
			write("services",payload);
			break;
		case "messages":
			write("messages",payload);
			break;
		default:
			log.error("Channel does not exist: " + channel);
		}
		} catch (NamingException e) {
			e.printStackTrace();
		}
	}
}
