package net.ion.craken.node.search;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherCacheStore;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;

public class TestBaseSearch extends TestCase {

	protected RepositoryImpl r;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		// GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("crakensearch").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		// this.r = RepositoryImpl.create(gconfig).forSearch() ;
		this.r = RepositoryImpl.create();
		r.defineConfig("test.node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
//				.eviction().maxEntries(10000)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new ISearcherCacheStore()).addProperty("location","./resource/local")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		r.start() ;
		
		this.session = r.testLogin("test");
	}

	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown();
		super.tearDown();
	}
	
}
