package life.genny.qwanda.service;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import com.hazelcast.config.NetworkConfig;
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
    cfg.getGroupConfig().setName((System.getenv("USER")==null?"GENNY":System.getenv("USER")));
    cfg.getGroupConfig().setPassword((System.getenv("USER")==null?"GENNY":System.getenv("USER")));

    HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
    mapBaseEntitys = instance.getMap("BaseEntitys");
  }

}
