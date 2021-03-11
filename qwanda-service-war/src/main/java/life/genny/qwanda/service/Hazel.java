package life.genny.qwanda.service;



import java.lang.invoke.MethodHandles;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;

import com.hazelcast.core.HazelcastInstance;

import org.apache.logging.log4j.Logger;
import org.infinispan.client.hotrod.DefaultTemplate;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.client.hotrod.impl.ConfigurationProperties;
import org.infinispan.commons.api.CacheContainerAdmin;

import life.genny.qwandautils.GennySettings;

@Singleton
public class Hazel {
	
	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());

    HazelcastInstance instance;

//    private IMap mapBaseEntitys;

//    /**
//     * @return the mapBaseEntitys
//     */
//    public IMap getMapBaseEntitys() {
//        return mapBaseEntitys;
//    }

    private Set<String> realms = new HashSet<String>();

    private Map<String, RemoteCache> caches= new HashMap<>();
    public RemoteCache<String, String> getMapBaseEntitys(final String realm) {
      if(realms.contains(realm)){
       return caches.get(realm); 
      }else{
	      cacheManager.administration().withFlags(CacheContainerAdmin.AdminFlag.VOLATILE).getOrCreateCache(realm, DefaultTemplate.DIST_SYNC);
        realms.add(realm);
        caches.put(realm,cacheManager.getCache(realm));
        return caches.get(realm); 
      }
    }

//    /**
//     * @param mapBaseEntitys the mapBaseEntitys to set
//     */
//    public void setMapBaseEntitys(final IMap mapBaseEntitys) {
//        this.mapBaseEntitys = mapBaseEntitys;
//    }

//    public static HazelcastInstance getHazelcastClientInstance(){
//        ClientConfig cfg = new ClientConfig();
//        cfg.addAddress(GennySettings.cacheServerName);
//        cfg.getGroupConfig().setName(GennySettings.username);
//        cfg.getGroupConfig().setPassword(GennySettings.username);
//        HazelcastInstance haInst = HazelcastClient.newHazelcastClient(cfg);
//        return haInst;
//    }
    //public static HazelcastInstance getHazelcastServerInstance(){
        //Config cfg = new Config();
        //cfg.getGroupConfig().setName(GennySettings.username);
        //cfg.getGroupConfig().setPassword(GennySettings.username);

        //return Hazelcast.newHazelcastInstance(cfg);
    //}
	  private  RemoteCacheManager cacheManager ;
    @PostConstruct
    public void init() {
	      ConfigurationBuilder builder = new ConfigurationBuilder();
	      builder.addServer()
	               .host(GennySettings.cacheServerName)
	               .port(ConfigurationProperties.DEFAULT_HOTROD_PORT)
	             .security().authentication()
	               //Add user credentials.
	               .username(System.getenv("INFINISPAN_USERNAME"))
	               .password(System.getenv("INFINISPAN_PASSWORD"))
	               .realm("default")
	               .saslMechanism("DIGEST-MD5");
        cacheManager = new RemoteCacheManager(builder.build());
        //if(GennySettings.isCacheServer){
          //log.info("Is A Cache Server");
            //instance = getHazelcastServerInstance();
        //}else{
          //log.error("This service is configured to run only as a Cache Server");
////            instance = getHazelcastClientInstance();
////
        //}

 //       mapBaseEntitys = instance.getMap(GennySettings.mainrealm); // To fix
    }

}
