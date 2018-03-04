package life.genny.qwanda.service;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@Singleton
public class Hazel {

  private IMap mapBaseEntitys;

  /**
   * @return the mapBaseEntitys
   */
  public IMap getMapBaseEntitys() {
    return mapBaseEntitys;
  }

  /**
   * @param mapBaseEntitys the mapBaseEntitys to set
   */
  public void setMapBaseEntitys(final IMap mapBaseEntitys) {
    this.mapBaseEntitys = mapBaseEntitys;
  }

  @PostConstruct
  public void init() {
    Config cfg = new Config();
    cfg.setInstanceName("gennyql");
    cfg.getGroupConfig().setName("clusterwidemap");
    cfg.getGroupConfig().setPassword("cluster");
    HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
    mapBaseEntitys = instance.getMap("BaseEntitys");

  }

}
