package life.genny.qwanda.endpoint;


import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import javax.ejb.Stateless;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import io.swagger.annotations.Api;
import life.genny.qwandautils.GitUtils;



/**
 * Version endpoint
 *
 * @author Adam Crow
 */

@Path("/version")
@Api(value = "/version", description = "Version", tags = "version")
@Produces(MediaType.APPLICATION_JSON)

@Stateless


public class VersionEndpoint {
  /**
   * Stores logger object.
   */
  protected static final Logger log = org.apache.logging.log4j.LogManager
      .getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());
  
  @GET
  @Path("/")
  public Response version() {
    String versionString = "";
    try {
      MavenXpp3Reader reader = new MavenXpp3Reader();
      InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream("pom.xml");
      if(inputStream != null) {
        Model model = reader.read(new InputStreamReader(inputStream));
        versionString = GitUtils.getGitVersionString(model);
      }
      
    } catch (IOException | XmlPullParserException e) {
      log.error("Error generating version details", e);
    } 
    return Response.status(200).entity(versionString).build();
  }
}
