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
    Properties properties = new Properties();
    try {
      properties.load(Thread.currentThread().getContextClassLoader().getResource("git.properties")
          .openStream());
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    return Response.status(200).entity(properties).build();
  }



}
