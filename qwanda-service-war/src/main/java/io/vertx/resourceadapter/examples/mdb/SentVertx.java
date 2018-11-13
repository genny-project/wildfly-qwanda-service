package io.vertx.resourceadapter.examples.mdb;

import java.util.logging.Logger;
import javax.ejb.Singleton;
import javax.naming.NamingException;
import javax.resource.ResourceException;


@Singleton
// @Startup
public class SentVertx {

  private final Logger logger = Logger.getLogger(SentVertx.class.getName());

  // @PostConstruct
  public static void whatever() throws NamingException, ResourceException {
    javax.naming.InitialContext ctx = null;
    io.vertx.resourceadapter.VertxConnection conn = null;
    try {
      ctx = new javax.naming.InitialContext();
      final io.vertx.resourceadapter.VertxConnectionFactory connFactory =
          (io.vertx.resourceadapter.VertxConnectionFactory) ctx
          .lookup("java:/eis/VertxConnectionFactory");
      conn = connFactory.getVertxConnection();
      conn.vertxEventBus().send("cmds", "Hello from JCA");
    } catch (final Exception e) {
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

  public void whatevers() throws NamingException, ResourceException {
    // javax.naming.InitialContext ctx = null;
    // io.vertx.resourceadapter.VertxConnection conn = null;
    // try {
    // ctx = new javax.naming.InitialContext();
    // io.vertx.resourceadapter.VertxConnectionFactory connFactory =
    // (io.vertx.resourceadapter.VertxConnectionFactory) ctx
    // .lookup("java:/eis/VertxConnectionFactory");
    // conn = connFactory.getVertxConnection();
    // conn.vertxEventBus().send("inbound-address",
    // "Hello from JCA");
    // } catch (Exception e) {
    // System.out.println(e.getMessage());
    // System.out.flush();
    // } finally {
    // if (ctx != null) {
    // ctx.close();
    // }
    // if (conn != null) {
    // conn.close();
    // }
    // }
  }
}
