package com.chatbot.util;

import java.util.concurrent.TimeUnit;

import org.ehcache.Cache;
import org.ehcache.CacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.expiry.Duration;
import org.ehcache.expiry.Expirations;


public class CacheHelper <K extends Object,V extends Object> {
	
	private CacheManager cacheManager;
		
	private Cache <K,V> cache;
	
	public CacheHelper(String cacheAlias){
		cacheManager=CacheManagerBuilder.newCacheManagerBuilder().build();
		cacheManager.init();
		this.cache=CreateCacheObjcet(cacheAlias);
	}
	
	private Cache<K, V> CreateCacheObjcet(String cacheAlias){
		Cache<K,V> cachedObject = null;
		if(cacheAlias.equalsIgnoreCase("usersResponses")) {
		 cachedObject =
				(Cache<K, V>) cacheManager.createCache(
						cacheAlias, CacheConfigurationBuilder.newCacheConfigurationBuilder(
								Object.class,Object.class,  ResourcePoolsBuilder.heap(100)).withExpiry(
										Expirations.timeToLiveExpiration(Duration.of(3,TimeUnit.MINUTES))));
		}else {
		cachedObject =
					(Cache<K, V>) cacheManager.createCache(
							cacheAlias, CacheConfigurationBuilder.newCacheConfigurationBuilder(
									Object.class,Object.class,  ResourcePoolsBuilder.heap(100)).withExpiry(
											Expirations.timeToLiveExpiration(Duration.of(1,TimeUnit.MINUTES))));
		}
		return cachedObject;
	}
	
	public void addToCentralCache(K cachedKey, V cachedValue){
		this.cache.put(cachedKey, cachedValue);
	}
	
	public Object getCachedValue(K cachableKey){
		Object cachedValue=this.cache.get(cachableKey);
		return cachedValue;
	}
	

}
