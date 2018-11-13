package life.genny.qwanda.util;

import java.io.IOException;
import java.util.Properties;

import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.bean.ManagedBean;
//jms stuff
import javax.jms.JMSException;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

@ApplicationScoped
@ManagedBean(name = "wildflyJms", eager = true)
public class WildFlyJmsQueueSender {
  public final static String JMS_CONNECTION_FACTORY_JNDI = "jms/RemoteConnectionFactory";
  public final static String JMS_QUEUE_JNDI = "jms/queue/GennyQ";
  public final static String JMS_USERNAME = "jmsuser"; // The role for this user is "guest" in ApplicationRealm
  public final static String JMS_PASSWORD = "jmspassword1";
  public final static String WILDFLY_REMOTING_URL = "http-remoting://localhost:8080";

  private QueueConnectionFactory qconFactory;
  private QueueConnection qcon;
  private QueueSession qsession;
  private QueueSender qsender;
  private Queue queue;
  private TextMessage msg;

  public static int counter = 0;

  public static void main(final String[] args) throws Exception {
    final InitialContext ic = getInitialContext();
    final WildFlyJmsQueueSender queueSender = new WildFlyJmsQueueSender();
    queueSender.init(ic, JMS_QUEUE_JNDI);
    readAndSend(queueSender);
    queueSender.close();
  }

  // @PostConstruct
  public void initialise() {
    try {
      final InitialContext ic = getInitialContext();
      init(ic, JMS_QUEUE_JNDI);

    } catch (final NamingException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    } catch (final JMSException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

  @PreDestroy
  public void postDestruct() {
    try {
      close();
    } catch (final JMSException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  public void init(final Context ctx, final String queueName) throws NamingException, JMSException {
    qconFactory = (QueueConnectionFactory) ctx.lookup(JMS_CONNECTION_FACTORY_JNDI);

    // If you won't pass jms credential here then you will get
    // [javax.jms.JMSSecurityException: HQ119031: Unable to validate user: null]
    qcon = qconFactory.createQueueConnection(this.JMS_USERNAME, this.JMS_PASSWORD);

    qsession = qcon.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
    queue = (Queue) ctx.lookup(queueName);
    qsender = qsession.createSender(queue);
    msg = qsession.createTextMessage();
    qcon.start();
  }

  public void send(final String message) throws JMSException, NamingException {
    final InitialContext ic = getInitialContext();
    init(ic, JMS_QUEUE_JNDI);

    msg.setText(message);
    msg.setIntProperty("counter", counter++);
    qsender.send(msg);
    close();
  }

  public void close() throws JMSException {
    qsender.close();
    qsession.close();
    qcon.close();
  }

  private static void readAndSend(final WildFlyJmsQueueSender wildFlyJmsQueueSender)
      throws IOException, JMSException, NamingException {
    final String line = "Test Message Body with counter = ";
    for (int i = 0; i < 10; i++) {
      wildFlyJmsQueueSender.send(line + i);
      System.out.println("JMS Message Sent: " + line + i + "\n");
    }
  }

  private static InitialContext getInitialContext() throws NamingException {
    InitialContext context = null;
    try {
      final Properties props = new Properties();
      props.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
      props.put(Context.PROVIDER_URL, WILDFLY_REMOTING_URL); // NOTICE: "http-remoting" and port "8080"
      props.put(Context.SECURITY_PRINCIPAL, JMS_USERNAME);
      props.put(Context.SECURITY_CREDENTIALS, JMS_PASSWORD);
      // props.put("jboss.naming.client.ejb.context", true);
      context = new InitialContext(props);
      System.out.println("\n\tGot initial Context: " + context);
    } catch (final Exception e) {
      e.printStackTrace();
    }
    return context;
  }

  public void sendMessage(final String message) throws Exception {

    final InitialContext ic = getInitialContext();
    final WildFlyJmsQueueSender queueSender = new WildFlyJmsQueueSender();
    queueSender.init(ic, JMS_QUEUE_JNDI);
    readAndSend(queueSender);
    queueSender.close();

  }
}