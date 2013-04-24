package net.ion.craken.tree;

import junit.framework.TestCase;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestGraph  extends TestCase  {

	private TreeCache tree;
	private Cache<Object, Object> cache;

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
		tree.stop() ;
		super.tearDown();
	}
	
	
	public void testCreate() throws Exception {
		TreeNode root = tree.getRoot();
		
		
	}
	
	
}
