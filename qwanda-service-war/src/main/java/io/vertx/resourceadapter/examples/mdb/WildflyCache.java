package io.vertx.resourceadapter.examples.mdb;


import java.util.concurrent.TimeUnit;
import javax.enterprise.context.ApplicationScoped;
import java.lang.invoke.MethodHandles;
import org.apache.logging.log4j.Logger;
import java.io.IOException;

import javax.inject.Inject;

import life.genny.channel.DistMap;

import life.genny.eventbus.WildflyCacheInterface;
import life.genny.qwanda.service.Hazel;
import life.genny.qwandautils.GennySettings;
import io.vertx.core.json.JsonObject;
import life.genny.qwandautils.QwandaUtils;

//@ApplicationScoped
public class WildflyCache implements WildflyCacheInterface {

	/**
	 * Stores logger object.
	 */
	protected static final Logger log = org.apache.logging.log4j.LogManager
			.getLogger(MethodHandles.lookup().lookupClass().getCanonicalName());


	Hazel inDb;
	
	public WildflyCache(Hazel inDb)
	{
		this.inDb = inDb;
	}

	@Override
	public Object readCache(String key, String token) {

		Object ret = inDb.getMapBaseEntitys().get(key);

		return ret;
	}

	@Override
	public void writeCache(String key, String value, String token,long ttl_seconds) {
		if (value == null) {
			
			inDb.getMapBaseEntitys().remove(key);
		} else {
			if (System.getenv("USE_VERTX_UTILS")!=null) {
				log.info("Writing directly to Cache :"+key);
				inDb.getMapBaseEntitys().put(key, value);
			} else {
				JsonObject json = new JsonObject();
				json.put("key", key);
				json.put("json", value);
				try {					
					QwandaUtils.apiPostEntity(GennySettings.ddtUrl + "/write", json.toString(), token);
				} catch (IOException e) {
					log.error("Could not write to cache");
				}
			}
		}
	}

	@Override
	public void clear() {
		inDb.getMapBaseEntitys().clear();
		
	}




}
