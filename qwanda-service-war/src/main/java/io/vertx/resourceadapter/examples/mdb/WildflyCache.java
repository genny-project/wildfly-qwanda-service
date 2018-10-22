package io.vertx.resourceadapter.examples.mdb;


import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import life.genny.channel.DistMap;
import life.genny.qwanda.service.Hazel;
import life.genny.qwandautils.GennyCacheInterface;
import life.genny.qwandautils.GennySettings;

public class WildflyCache implements GennyCacheInterface {
	
	Hazel inDb;
	
	public WildflyCache(Hazel inDb)
	{
		this.inDb = inDb;
		inDb.init();
		
	}

	@Override
	public Object readCache(String key, String token) {
		return inDb.getMapBaseEntitys(GennySettings.mainrealm).get(key);
	}

	@Override
	public void writeCache(String key, String value, String token,long ttl_seconds) {
		if (value == null) {
			
			inDb.getMapBaseEntitys(GennySettings.mainrealm).remove(key);
		} else {
			inDb.getMapBaseEntitys(GennySettings.mainrealm).put(key, value);
		}
	}

	@Override
	public void clear() {
		inDb.getMapBaseEntitys(GennySettings.mainrealm).clear();
		
	}




}
