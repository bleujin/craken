package net.ion.bleujin;

import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;

public class TestDistConfig extends TestCase{

	public void testDisConfig() throws Exception {
		DefaultCacheManager dm = new DefaultCacheManager("./resource/config/working-cache-config.xml") ;
		
		Cache<Object, Object> notdefine = dm.getCache("notdefined") ;
		Debug.line(notdefine.getCacheConfiguration().clustering()) ;
		
		
		dm.defineConfiguration("newdefine", new ConfigurationBuilder().clustering().cacheMode(CacheMode.LOCAL).build()) ;
		Cache<Object, Object> newdefine = dm.getCache("newdefine") ;
		Debug.line(newdefine.getCacheConfiguration().clustering()) ;
		
		
		
		dm.stop(); 
	}
}
