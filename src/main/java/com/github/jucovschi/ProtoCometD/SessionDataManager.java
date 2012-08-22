package com.github.jucovschi.ProtoCometD;

import java.util.Random;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;

import com.google.protobuf.AbstractMessage;

public class SessionDataManager {
	private final Cache sessionDataCache;

	static private SessionDataManager defaultManager;
	Random rand;
	
	public SessionDataManager() {
		rand = new Random();
		CacheManager cacheManager = CacheManager.getInstance();
		int oneDay = 24 * 60 * 60;
		Cache _cache = cacheManager.getCache("SessionData");
		if (_cache == null) {
			_cache = new Cache("SessionData", 200, false, false, oneDay, oneDay);		
			cacheManager.addCache(_cache);	
		}
		sessionDataCache = _cache;
	}
	
	static public SessionDataManager getInstance() {
		if (defaultManager == null)
			defaultManager = new SessionDataManager();
		return defaultManager;
	}
	
	public String newActionToken(AbstractMessage data) {
		String token = Long.toString(rand.nextLong());
		sessionDataCache.put(new Element(token, data));
		return token;
	}
	
	boolean validToken(String validToken) {
		if (validToken == null)
			return false;
		return validToken.matches("[\\w\\-]{5,100}");
	}
	
	public <T extends AbstractMessage> T getDataOrInit(String token, T initVal) {
		if (!validToken(token))
			return null;
		if (sessionDataCache.isKeyInCache(token)) {
			Object val = sessionDataCache.get(token).getObjectValue();
			if (initVal.getClass().isAssignableFrom(val.getClass())) {
				return (T) val;
			} else
				return initVal;
		}
		return initVal;
	}
	
	public Object getData(String token) {
		if (!validToken(token))
			return null;
		if (sessionDataCache.isKeyInCache(token)) {
			return sessionDataCache.get(token).getObjectValue();
		}
		return null;
	}
	
	public <T extends AbstractMessage> T getData(String token, Class<T> cls) {
		if (!validToken(token))
			return null;
		if (sessionDataCache.isKeyInCache(token)) {
			Object obj = sessionDataCache.get(token).getObjectValue();
			if (cls.isAssignableFrom(obj.getClass())) {
				return (T) obj;
			}
		}
		return null;
	}

}
