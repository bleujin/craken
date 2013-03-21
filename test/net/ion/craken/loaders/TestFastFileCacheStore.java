package net.ion.craken.loaders;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestFastFileCacheStore extends TestCase {

	private GlobalConfiguration globalConf;
	private DefaultCacheManager dftManager;
	private Configuration defaultConf ;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml").build();
		this.defaultConf = createFastLocalCacheStore() ;
		dftManager = new DefaultCacheManager(globalConf, defaultConf, false);
		dftManager.start() ;
	}

	@Override
	protected void tearDown() throws Exception {
		dftManager.stop();
		super.tearDown();
	}
	
	public void testPut() throws Exception {
		Cache<String, Employee> cache = dftManager.getCache("employee");
//		cache.clear() ;
		for (int i = 0; i < 10; i++) {
			cache.put("test" + i, Employee.createEmp(20, "test" + i, 7789 + i));
		}

		Debug.line(cache.keySet().size()) ;
	}

	private static org.infinispan.configuration.cache.Configuration createFastLocalCacheStore() {
		return new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching().clustering().hash().numOwners(2).unsafe()
		// .eviction().maxEntries(1000)
				.invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location", "./resource/temp")
				// ./resource/temp
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build();
	}

}
