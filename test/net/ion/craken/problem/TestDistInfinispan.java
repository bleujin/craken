package net.ion.craken.problem;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

@Listener
public class TestDistInfinispan extends TestCase{

	
	public void testFirstServer() throws Exception {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().build() ;
		DefaultCacheManager dm = new DefaultCacheManager(gconfig) ;
		
		dm.defineConfiguration("test", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).build()) ;
		Cache<String, Object> cache = dm.getCache("test") ;
		cache.addListener(this);
		
		new InfinityThread().startNJoin(); 
		
		cache.stop(); 
		dm.stop(); 
	}
	
	public void testSecondServer() throws Exception {

		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().build() ;
		DefaultCacheManager dm = new DefaultCacheManager(gconfig) ;
		
		dm.defineConfiguration("test", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).build()) ;
		Cache<String, Object> cache = dm.getCache("test") ;
		cache.addListener(this);
		
		cache.put("bleujin", "Hello World") ;

		new InfinityThread().startNJoin(); 
		
		cache.stop(); 
		dm.stop(); 
	}
	
	
	@CacheEntryModified
	public void modified(CacheEntryModifiedEvent<String, Object> event){
		if (event.isPre()) return ;
		Debug.line(event.getKey(), event.getValue());
	}
	
	
}
