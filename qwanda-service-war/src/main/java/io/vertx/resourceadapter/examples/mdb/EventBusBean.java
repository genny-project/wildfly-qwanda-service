package io.vertx.resourceadapter.examples.mdb;

import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.naming.NamingException;
import javax.resource.ResourceException;
import io.vertx.resourceadapter.*;
import life.genny.channel.Producer;
import life.genny.eventbus.EventBusInterface;
import life.genny.qwanda.entity.BaseEntity;



public class EventBusBean implements EventBusInterface {

  private Logger logger = Logger.getLogger(EventBusBean.class.getName());

	public void write(final String channel, final Object msg) throws NamingException 
	{
		   javax.naming.InitialContext ctx = null;
		    io.vertx.resourceadapter.VertxConnection conn = null;
		    try {
		      ctx = new javax.naming.InitialContext();
		      io.vertx.resourceadapter.VertxConnectionFactory connFactory =
		          (io.vertx.resourceadapter.VertxConnectionFactory) ctx
		              .lookup("java:/eis/VertxConnectionFactory");
		      conn = connFactory.getVertxConnection();
		      conn.vertxEventBus().publish(channel, msg);
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
		    		  
		    	  }
		      }
		    }
	}
  

	public void send(final String channel, final Object msg) throws NamingException 
	{
		   javax.naming.InitialContext ctx = null;
		    io.vertx.resourceadapter.VertxConnection conn = null;
		    try {
		      ctx = new javax.naming.InitialContext();
		      io.vertx.resourceadapter.VertxConnectionFactory connFactory =
		          (io.vertx.resourceadapter.VertxConnectionFactory) ctx
		              .lookup("java:/eis/VertxConnectionFactory");
		      conn = connFactory.getVertxConnection();
		      conn.vertxEventBus().send(channel, msg);
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
			payload = EventBusInterface.privacyFilter(user, payload,filterAttributes);
			write("cmds",payload);
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
