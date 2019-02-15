package life.genny.qwanda.util;

import java.util.Properties;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.bean.ManagedBean;
import javax.jms.Queue;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSender;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.naming.Context;
import javax.naming.InitialContext;

@ApplicationScoped
@ManagedBean(name = "wildflyJms2", eager = true)
public class WildflyJms {

	// Set the JNDI context factory for a JBOSS/ WildFly Server.
	public final static String JNDI_FACTORY = "org.jboss.ejb.client.naming";

	// Set the JMS context factory.
	public final static String JMS_FACTORY = "java:/ConnectionFactory";

	// Set the queue.
	public final static String QUEUE = "java:/GennyQ";

	// Set Wildfly URL.
	public final static String WildflyURL = "http-remoting://localhost:8080";

	public void send(final String message) throws Exception {

		// 1) Create and start a connection
		Properties properties = new Properties();
		properties.put(Context.URL_PKG_PREFIXES, JNDI_FACTORY);

		InitialContext ic = new InitialContext(properties);

		QueueConnectionFactory f = (QueueConnectionFactory) ic.lookup(JMS_FACTORY);

		QueueConnection con = f.createQueueConnection();
		con.start();

		// 2) create queue session
		QueueSession ses = con.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);

		// 3) get the Queue object
		Queue t = (Queue) ic.lookup(QUEUE);

		// 4)create QueueSender object
		QueueSender sender = ses.createSender(t);

		// 5) create TextMessage object
		TextMessage msg = ses.createTextMessage();
		msg.setText("This message will be send to a WildFly Queue !");

		// 6) send message
		sender.send(msg);
		System.out.println("Message successfully sent to a WildFly Queue.");

		// 7) connection close
		con.close();

	}

}