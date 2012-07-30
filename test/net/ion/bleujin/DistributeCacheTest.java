package net.ion.bleujin;


import java.util.Properties;

import junit.framework.TestCase;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.jgroups.tests.perf.transports.JGroupsTransport;

public class DistributeCacheTest extends TestCase {
	
	public void testCreate() throws Exception {
		GlobalConfiguration gc = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-tcp.xml").build() ;
		Configuration c = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC)
			.jmxStatistics().enable()
			.clustering().l1().enable().lifespan(6000000).invocationBatching()
			.clustering().hash().numOwners(3).unsafe()
			.build() ;
			
		DefaultCacheManager dc = new DefaultCacheManager(gc, c, false);
		dc.start() ;

		Cache<String, Object> cache = dc.getCache("workspace") ;
		
		cache.start() ;

		cache.stop() ;
		dc.stop() ;
	}

}
