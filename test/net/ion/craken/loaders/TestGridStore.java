package net.ion.craken.loaders;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EvictionConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.transaction.TransactionMode;

import junit.framework.TestCase;

public class TestGridStore extends TestCase {
	
	
	public void testSet() throws Exception {
		DefaultCacheManager dm = new DefaultCacheManager() ;
		int maxEntry = 1000 ;

		EvictionConfigurationBuilder builder = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration())
			.transaction().transactionMode(TransactionMode.TRANSACTIONAL)
			.invocationBatching().enable()
			.persistence().addStore(GridLoaderConfigurationBuilder.class).maxEntries(maxEntry).fetchPersistentState(true).preload(false).shared(false).purgeOnStartup(false).ignoreModifications(false)
			.async().enabled(false).flushLockTimeout(20000).shutdownTimeout(1000).modificationQueueSize(1000).threadPoolSize(5)
			.eviction().maxEntries(maxEntry) ; // .eviction().expiration().lifespan(10, TimeUnit.SECONDS) ;
		dm.defineConfiguration("test", builder.build()) ;
		
		
		Cache<String, String> cache = dm.getCache("test");
		cache.start();
		
		cache.put("name", "bleujin") ;
	
		dm.stop();
	}

}
