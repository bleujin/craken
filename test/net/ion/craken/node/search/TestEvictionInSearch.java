package net.ion.craken.node.search;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherCacheStore;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;

public class TestEvictionInSearch extends TestCase{

	
	public void testEviction() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineConfig("test.node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
				.eviction().maxEntries(10)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new ISearcherCacheStore()).addProperty("location","./resource/local")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		r.start() ;

		
		ReadSession session = r.login("test");
		
		session.tranSync(TransactionJobs.dummyEmp(20)) ;
		
		
		session.pathBy("/emp").children().debugPrint() ;
		
		
		r.shutdown() ;
		
	}
}
