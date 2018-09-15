package life.genny.qwanda.endpoint;


import org.apache.logging.log4j.Logger;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Properties;
import io.swagger.annotations.Api;
import life.genny.qwandautils.GitUtils;



/**
 * Version endpoint
 *
 * @author Adam Crow
 */

@Path("/version")
@Api(value = "/version", tags = "version")
@Produces(MediaType.APPLICATION_JSON)

@Stateless


public class VersionEndpoint {
  /**
   * Stores logger object.
   */
  protected static final Logger log = org.apache.logging.log4j.LogManager
      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
  public static final String GIT_VERSION_PROPERTIES = "GitVersion.properties";
  
  public static final String PROJECT_DEPENDENCIES = "project_dependencies";
  
  @GET
  @Path("/")
  public Response version() {
    Properties properties = new Properties();
    String versionString = "";
    try {
      properties.load(Thread.currentThread().getContextClassLoader().getResource(GIT_VERSION_PROPERTIES)
          .openStream());
      String projectDependencies = properties.getProperty(PROJECT_DEPENDENCIES);
      versionString = GitUtils.getGitVersionString(projectDependencies);
    } catch (IOException e) {
      log.error("Error reading GitVersion.properties", e);
    }

    return Response.status(200).entity(versionString).build();
  }



}
