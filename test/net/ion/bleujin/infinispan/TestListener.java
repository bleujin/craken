package net.ion.bleujin.infinispan;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

public class TestListener extends TestCase {

	
	
	public void testRun() throws Exception {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build() ;
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig);
		dcm.defineConfiguration("transaction", new ConfigurationBuilder().build()) ;
		
		dcm.addListener(new CacheListener()) ;
		dcm.start() ;
		
		Cache<String, String> cache = dcm.getCache("transaction");
//		cache.addListener(new CacheListener()) ;
		
		cache.put("bleujin", "entry bleujin") ;
		
		cache.remove("bleujin") ;
		new InfinityThread().startNJoin(); 
		dcm.stop() ;
	}

	
	

	public void testRun2() throws Exception {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build() ;
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig);
		dcm.defineConfiguration("transaction", new ConfigurationBuilder().build()) ;
		
		dcm.addListener(new CacheListener()) ;
		dcm.start() ;
		
		Cache<String, String> cache = dcm.getCache("transaction");
//		cache.addListener(new CacheListener()) ;
		
		cache.put("bleujin", "entry bleujin") ;
		
		cache.remove("bleujin") ;
		new InfinityThread().startNJoin(); 
		dcm.stop() ;
	}

	
	
	
	
	@Listener
	public static class CacheListener {
		
		@CacheStarted
		public void cacheStarted(CacheStartedEvent e){
			Debug.line(e.getCacheName(), e.getCacheManager().getMembers()) ;
		}
		

		@ViewChanged
		public void view(ViewChangedEvent e){
			Debug.line(e) ;
		}
//		
//		@CacheEntryCreated
//		public void cacheEntryCreated(CacheEntryCreatedEvent<String, String> e){
//			Debug.line(e.getKey(), e.getType());
//		}
//		
//		@CacheEntryModified
//		public void cacheEntryModified(CacheEntryModifiedEvent<String, String> e){
//			if (e.isPre()) return ;
//			Debug.line(e.getKey(), e.getValue(), e) ;
//		}
//		
//		@CacheEntryRemoved
//		public void cacheEntryRemoved(CacheEntryRemovedEvent<String, String> e){
//			if (! e.isPre()) return ;
//			Debug.line(e.getKey(), "removed", e.getValue());
//		}
	}
}
