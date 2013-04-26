package net.ion.craken.tree;

import junit.framework.TestCase;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestPathGeneric extends TestCase {

	private TreeCache<PropertyId, PropertyValue> tree;
	private Cache<PropertyId, PropertyValue> cache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Configuration config = new ConfigurationBuilder().invocationBatching().enable().build() ; // not indexable : indexing().enable().
		final DefaultCacheManager dm = new DefaultCacheManager(config);
		dm.start() ;
		this.cache = dm.getCache() ;
		this.tree = new TreeCacheFactory().createTreeCache(cache) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.cache.stop() ;
		super.tearDown();
	}
	

	public void testCreate() throws Exception {
		
	}
	
	
}
