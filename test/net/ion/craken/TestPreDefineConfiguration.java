package net.ion.craken;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;

public class TestPreDefineConfiguration extends TestBase {

	public void testPreDefine() throws Exception {
		craken.preDefineConfig(Employee.class, new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_ASYNC).jmxStatistics().enable().clustering().l1().enable().lifespan(Long.MAX_VALUE).build());
		
		LegContainer<Employee> econtainer = craken.defineLeg(Employee.class);
		
		Configuration config = econtainer.getCacheConfiguration() ;
		assertEquals(true, config.clustering().l1().enabled()) ;
	}
	
	
}
