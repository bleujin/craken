package net.ion.craken.tree;

import junit.framework.TestCase;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestPathGeneric extends TestCase {

	private TreeCache<PropertyId, PropertyValue> tree;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Configuration config = new ConfigurationBuilder().invocationBatching().enable().build() ; // not indexable : indexing().enable().
		final DefaultCacheManager dm = new DefaultCacheManager(config);
		this.tree = new TreeCacheFactory().createTreeCache(dm, "test") ;
		dm.start() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		tree.stop() ;
		super.tearDown();
	}
	

	public void testCreate() throws Exception {
		
	}
	
	
}
