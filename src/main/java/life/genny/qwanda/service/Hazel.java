package life.genny.qwanda.service;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.client.config.ClientNetworkConfig;
import com.hazelcast.config.Config;
import com.hazelcast.config.NetworkConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.IMap;

@Singleton
public class Hazel {
	
	  private static String hostIP =
		      System.getenv("HOSTIP") != null ? System.getenv("HOSTIP") : "127.0.0.1";
		  
		  private static String systemUser =
		    	      System.getenv("USER") != null ? System.getenv("USER") : "genny";


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
    cfg.getGroupConfig().setName( hostIP ).setPassword( systemUser);

    NetworkConfig network = cfg.getNetworkConfig();
  // ports between 35000 and 35100
    network.addOutboundPortDefinition("45000-45200");

    HazelcastInstance instance = Hazelcast.newHazelcastInstance(cfg);
     mapBaseEntitys = instance.getMap("BaseEntitys");
    
  


  }

}
