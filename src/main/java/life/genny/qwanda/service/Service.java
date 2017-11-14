package life.genny.qwanda.service;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import life.genny.daoservices.BaseEntityService;
import life.genny.qwanda.util.PersistenceHelper;

@ApplicationScoped
public class Service extends BaseEntityService {

  public Service() {
    // TODO Auto-generated constructor stub
  }

  @Inject
  private PersistenceHelper helper;

  @PostConstruct
  public void init() {

    this.setEm(helper.getEntityManager());
  }

}
