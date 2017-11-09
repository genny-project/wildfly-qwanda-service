package life.genny.qwanda.service;

import java.lang.invoke.MethodHandles;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import org.apache.logging.log4j.Logger;
import life.genny.daoservices.BatchLoading;
import life.genny.qwanda.entity.BaseEntity;
import life.genny.qwanda.util.PersistenceHelper;

/**
 * This Service bean demonstrate various JPA manipulations of {@link BaseEntity}
 *
 * @author Adam Crow
 */
@Singleton
@Startup
public class StartupService {

  /**
   * Stores logger object.
   */
  protected static final Logger log = org.apache.logging.log4j.LogManager
      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

  // private final static String secret = System.getenv("GOOGLE_CLIENT_SECRET");
  // private final static String genny = System.getenv("GOOGLE_SHEETID");
  // private final static String channel40 = "1jNe-MOXx8DFxA2kDeCHjdZ7U-4Fqk1cqyhTgsZvUZ4o";
  // static File gennyPath = new File(System.getProperty("user.home"), ".credentials/genny");
  // static File channelPath = new File(System.getProperty("user.home"), ".credentials/channel");

  // private final String g = System.getenv("GOOGLE_CLIENT_SECRET");
  // private final String go = System.getenv("GOOGLE_SHEETID");
  // File credentialPath = new File(System.getProperty("user.home"),
  // ".credentials/sheets.googleapis.com-java-quickstart");

  @Inject
  private SecurityService securityService;

  @Inject
  private PersistenceHelper helper;

  @PostConstruct
  public void init2() {
    securityService.setImportMode(true); // ugly way of getting past security

    BatchLoading bl = new BatchLoading(helper.getEntityManager());
    bl.persistProject();

    securityService.setImportMode(false);
  }
}
