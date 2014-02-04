package net.ion.bleujin.infinispan;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;

public class TestListener extends TestCase {

	
	
	public void testRun() throws Exception {
		DefaultCacheManager dcm = new DefaultCacheManager();
		dcm.defineConfiguration("transaction", new ConfigurationBuilder().build()) ;
		
		dcm.start() ;
//		dcm.addListener(new CacheListener()) ;
		
		Cache<String, String> cache = dcm.getCache("transaction");
		cache.addListener(new CacheListener()) ;
		
		cache.put("bleujin", "entry bleujin") ;
		
		cache.remove("bleujin") ;
		dcm.stop() ;
	}

	
	
	
	
	@Listener
	public static class CacheListener {
		
		@CacheStarted
		public void cacheStarted(CacheStartedEvent e){
			Debug.line(e.getCacheName(), e.getCacheManager().getMembers()) ;
		}
		
		
		@CacheEntryCreated
		public void cacheEntryCreated(CacheEntryCreatedEvent<String, String> e){
			Debug.line(e.getKey(), e.getType());
		}
		
		@CacheEntryModified
		public void cacheEntryModified(CacheEntryModifiedEvent<String, String> e){
			if (e.isPre()) return ;
			Debug.line(e.getKey(), e.getValue(), e) ;
		}
		
		@CacheEntryRemoved
		public void cacheEntryRemoved(CacheEntryRemovedEvent<String, String> e){
			if (! e.isPre()) return ;
			Debug.line(e.getKey(), "removed", e.getValue());
		}
	}
}
