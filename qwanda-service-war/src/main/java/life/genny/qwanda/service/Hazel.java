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

//import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizeConfig;
import com.hazelcast.config.MaxSizeConfig.MaxSizePolicy;
import com.hazelcast.config.MultiMapConfig;
import com.hazelcast.config.SemaphoreConfig;
//import com.hazelcast.core.Hazelcast;
//import com.hazelcast.core.HazelcastInstance;
//import com.hazelcast.client.HazelcastClient;
import life.genny.qwandautils.GennySettings;

import org.apache.logging.log4j.Logger;
import java.lang.invoke.MethodHandles;

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

    public IMap getMapBaseEntitys(final String realm) {
        return  instance.getMap(realm);
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
    public static HazelcastInstance getHazelcastServerInstance(){
        Config cfg = new Config();
        cfg.getGroupConfig().setName(GennySettings.username);
        cfg.getGroupConfig().setPassword(GennySettings.username);

        return Hazelcast.newHazelcastInstance(cfg);
    }

    @PostConstruct
    public void init() {

        if(GennySettings.isCacheServer){
        	log.info("Is A Cache Server");
            instance = getHazelcastServerInstance();
        }else{
        	log.error("This service is configured to run only as a Cache Server");
//            instance = getHazelcastClientInstance();
//
        }

 //       mapBaseEntitys = instance.getMap(GennySettings.mainrealm); // To fix
    }

}
