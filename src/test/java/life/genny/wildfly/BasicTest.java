package life.genny.wildfly;

import static org.junit.Assert.assertEquals;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.EmptyAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;

import life.genny.qwanda.model.MyBean;
import life.genny.qwanda.model.Person;

//@RunWith(Arquillian.class)
public class BasicTest {

	@PersistenceContext
	private EntityManager entityManager;

	@Deployment
	public static WebArchive createDeployment() {
		// File[] files =
		// Maven.resolver().loadPomFromFile("pom.xml").importRuntimeDependencies().resolve()
		// .withTransitivity().asFile();

		return ShrinkWrap.create(WebArchive.class).addClass(Greeter.class).addPackage(MyBean.class.getPackage())
				.addPackage(Person.class.getPackage()).addAsResource("test-persistence.xml", "META-INF/persistence.xml")
				.addAsWebInfResource("wildfly-ds.xml").addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml")
		// .addAsLibraries(files)
		;

		// return
		// ShrinkWrap.create(WebArchive.class).addClass(Greeter.class).addPackage("life.genny.qwanda.endpoint")
		// .addClass(Logger.class).addClass(LogManager.class).addPackage("org.apache.logging.log4j")
		// .addPackage("org.apache.logging.log4j.spi").addPackage("org.apache.logging.log4j.message")
		// .addPackage("org.apache.logging.log4j.status").addPackage("org.apache.logging.log4j.util")
		// .addPackage("org.apache.logging.log4j.simple").addPackage("life.genny.qwanda.providers")
		// .addPackage("life.genny.qwanda.model").addPackage("life.genny.qwanda.observers")
		// .addPackage("life.genny.qwanda.service").addPackage("life.genny.qwanda.util").addAsLibraries(files)
		// .addAsManifestResource(EmptyAsset.INSTANCE, "beans.xml")
		// .addAsResource("test-persistence.xml",
		// "META-INF/persistence.xml").addAsWebInfResource("wildfly-ds.xml")
		// .addAsWebInfResource(EmptyAsset.INSTANCE, "beans.xml");
	}

	//
	@Inject
	private Greeter greeter;

	// @Inject
	// private Service service;

	@Inject
	private MyBean testBean;

	//
	// @Test
	public void shouldBeAbleTo() {
		assertEquals("Hello, aliens!", greeter.createGreeting("aliens"));
	}

	// @Test
	public final void testGetPerson() throws Exception {
		testBean.savePerson(new Person("bob"));

		assertEquals("bob", testBean.getPerson(1).getName());
	}
}
