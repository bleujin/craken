package net.ion.craken.node.search;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.loaders.lucene.OldCacheStore;
import net.ion.craken.loaders.lucene.OldCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;

public class TestBaseSearch extends TestCase {

	protected RepositoryImpl r;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = RepositoryImpl.create();
		r.defineWorkspaceForTest("test", CentralCacheStoreConfig.createDefault()) ;
		
		r.start() ;
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown();
		super.tearDown();
	}
	
}
