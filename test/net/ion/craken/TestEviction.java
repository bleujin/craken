package net.ion.craken;


import net.ion.framework.util.Debug;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;

import junit.framework.TestCase;

public class TestEviction extends TestBase {

	public void testNoneStrategy() throws Exception {
		
		ConfigurationBuilder builder = new ConfigurationBuilder() ;
		builder.clustering().cacheMode(CacheMode.DIST_ASYNC).eviction().strategy(EvictionStrategy.NONE) ;
		LegContainer<Employee> leg = craken.defineLeg(Employee.class, builder.build());
		
		for (int i = 0; i < 100; i++) {
			leg.newInstance(i).name(i + "'s").save() ; 
			
		}
		assertEquals(true, leg.keySet().size() == 100) ; 
	}
	

	
	public void testMaxEntry() throws Exception {
		ConfigurationBuilder builder = new ConfigurationBuilder() ;
		
		builder.clustering().cacheMode(CacheMode.DIST_ASYNC).eviction().strategy(EvictionStrategy.LRU).maxEntries(10) ;
		LegContainer<Employee> leg = craken.defineLeg(Employee.class, builder.build());
		
		for (int i = 0; i < 100; i++) {
			leg.newInstance(i).name(i + "'s").save() ; 
			assertEquals(true, leg.keySet().size() <= 10) ; 
			
		}
	}
	
	public void xtestExprire() throws Exception {
		ConfigurationBuilder builder = new ConfigurationBuilder() ;
		builder.clustering().cacheMode(CacheMode.DIST_ASYNC).expiration().lifespan(3000); 
		LegContainer<Employee> leg = craken.defineLeg(Employee.class, builder.build());
		
		for (int i = 0; i < 100; i++) {
			leg.newInstance(i).name(i + "'s").save() ;
			Thread.sleep(200) ;
			assertEquals(true, leg.keySet().size() <= 10) ; 
		}
	}
	
}
