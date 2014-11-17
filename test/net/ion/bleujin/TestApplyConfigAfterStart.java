package net.ion.bleujin;

import junit.framework.TestCase;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestApplyConfigAfterStart extends TestCase {

	
	public void testApplied() throws Exception {
		DefaultCacheManager dm = new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
		dm.defineConfiguration("created", new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).build()) ;
		
		dm.start(); 
		
		assertEquals(CacheMode.DIST_SYNC, dm.getCacheConfiguration("created").clustering().cacheMode());
		assertEquals(CacheMode.DIST_SYNC, dm.getDefaultCacheConfiguration().clustering().cacheMode());
		dm.stop(); 
	}

	public void testAppliedAfterStart() throws Exception {
		DefaultCacheManager dm = new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
		dm.start();
		
		dm.defineConfiguration("created", new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration()).build()) ;
		
		assertEquals(CacheMode.DIST_SYNC, dm.getCache("created").getCacheConfiguration().clustering().cacheMode());
		assertEquals(CacheMode.DIST_SYNC, dm.getDefaultCacheConfiguration().clustering().cacheMode());
		dm.stop(); 
	}
	
	public void testRedefine() throws Exception {
		DefaultCacheManager dm = new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
		dm.start();
		
		dm.defineConfiguration("created", new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration())	
				.clustering().l1().enabled(false).clustering().cacheMode(CacheMode.LOCAL).build()) ;
		assertEquals(CacheMode.LOCAL, dm.getCache("created").getCacheConfiguration().clustering().cacheMode());
		
	}

}
