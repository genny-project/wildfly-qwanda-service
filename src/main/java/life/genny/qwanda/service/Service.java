package life.genny.qwanda.service;

import javax.inject.Inject;
import life.genny.daoservices.BaseEntityService;
import life.genny.qwanda.util.PersistenceHelper;

public class Service {

  @Inject
  private PersistenceHelper helper;

  BaseEntityService service;

  public BaseEntityService getService() {
    return service;
  }

  public void setService(final BaseEntityService service) {
    this.service = service;
  }

  /**
   * @return
   */
  public static BaseEntityService getBaseEntityService() {
    Service ser = new Service();
    ser.setService(new BaseEntityService(ser.helper.getEntityManager()));
    return ser.getService();
  }
}
