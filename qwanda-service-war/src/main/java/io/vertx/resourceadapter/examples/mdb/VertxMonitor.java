package io.vertx.resourceadapter.examples.mdb;

import java.util.logging.Logger;
import javax.ejb.MessageDriven;
import io.vertx.core.eventbus.Message;
import io.vertx.resourceadapter.inflow.VertxListener;



/**
 * Message-Driven Bean implementation class for: VertxMonitor
 */
@MessageDriven(name = "VertxMonitor", messageListenerInterface = VertxListener.class)
//@MessageDriven(name = "VertxMonitor", messageListenerInterface = VertxListener.class, activationConfig = { @ActivationConfigProperty(propertyName = "address", propertyValue = "inbound-address"), })

public class VertxMonitor implements VertxListener {

  final static String st = System.getenv("MYIP");
  private final Logger logger = Logger.getLogger(VertxMonitor.class.getName());

  /**
   * Default constructor.
   */
  public VertxMonitor() {
    logger.info("VertxMonitor started.");
  }

  @Override
  public <T> void onMessage(Message<T> message) {
    message.body().toString();
    logger.info("Get a aaaaaaaaaaaaaaaaaaaamessage from Vert.x: " + message.body());
    // T body = message.body();
    // if (body != null) {
    // logger.info("Body of the message: " + body.toString());
    //
    // if (message.replyAddress() != null) {
    // message.reply("Hi, Got your message: " + body.toString());
    // } else{
    // logger.info("No reply address for message. Not responding!");
    // }
    // } else {
    // message.reply("Hi, Got your empty message.");
    // }
  }

}
