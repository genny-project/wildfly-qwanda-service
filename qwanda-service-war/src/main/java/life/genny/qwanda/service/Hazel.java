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
import java.io.File;
import java.lang.ClassLoader;

import life.genny.qwandautils.GennySettings;

@Singleton
public class Hazel {

	 HazelcastInstance instance;
	 
  private IMap mapBaseEntitys;

  /**
   * @return the mapBaseEntitys
   */
  public IMap getMapBaseEntitys() {
    return mapBaseEntitys;
  }
  
  public IMap getMapBaseEntitys(final String realm) {
	    return  instance.getMap(realm);
	  }

  /**
   * @param mapBaseEntitys the mapBaseEntitys to set
   */
  public void setMapBaseEntitys(final IMap mapBaseEntitys) {
    this.mapBaseEntitys = mapBaseEntitys;
  }

  @PostConstruct
  public void init() {
    Config config = new Config();
    config.getGroupConfig().setName(GennySettings.username);
    config.getGroupConfig().setPassword(GennySettings.username);

     
    config.setInstanceName("wildfly-qwanda-service");
    config.getNetworkConfig().setPortAutoIncrement(true);
    config.getNetworkConfig().setPort(5701);
    config.getNetworkConfig().getJoin().getMulticastConfig().setMulticastPort(54327).setMulticastGroup("224.2.2.3").setEnabled(true);
  //  config.getNetworkConfig().getJoin().getTcpIpConfig().setEnabled(true);
  //  config.getNetworkConfig().getJoin().getTcpIpConfig().addMember("127.0.0.1");
//
////    SSLConfig sslConfig = new SSLConfig();
////    sslConfig.setEnabled(false);
////    config.getNetworkConfig().setSSLConfig(sslConfig);
//
    instance = Hazelcast.newHazelcastInstance(config);
    mapBaseEntitys = instance.getMap(GennySettings.mainrealm); // To fix
  }

}
