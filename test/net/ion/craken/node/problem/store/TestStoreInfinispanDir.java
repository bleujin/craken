package net.ion.craken.node.problem.store;

import java.io.IOException;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.db.Page;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.reader.InfoReader.InfoHandler;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;

public class TestStoreInfinispanDir extends TestCase {

	private ReadSession session;
	private RepositoryImpl r;



	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
		
//		r.defineConfig("test.meta", TestConfig.createOldSearchCacheStore(1000));
//		r.defineConfig("test.chunks", TestConfig.createOldSearchCacheStore(10));
//		r.defineConfig("test.locks", TestConfig.createOldSearchCacheStore(1000));
		String wsname = "test" ;
		r.dm().defineConfiguration(wsname + ".node", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC)
				.clustering().eviction().maxEntries(10000).invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).addCacheLoader()
				.cacheLoader(new FastFileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		r.dm().defineConfiguration(wsname + ".meta", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).addCacheLoader()
				.cacheLoader(new FastFileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		r.dm().defineConfiguration(wsname + ".chunks", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().eviction().maxEntries(10).invocationBatching().enable().loaders().preload(true).shared(false).passivation(
				false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		r.dm().defineConfiguration(wsname + ".locks", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).build());

		
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	
	public void testLoadCentral() throws Exception {
		
		Central central = session.central();
//		index(central);
		
		Searcher searcher = central.newSearcher();
//		searcher.createRequest("name:bleujin").find().debugPrint();
		long start = System.currentTimeMillis();
		// int totalCount = session.createRequest("").page(Page.ALL).find().predicated(ReadNodePredicate.belowAt("/copy2")).size() ;
		Debug.line(searcher.createRequest("").find().totalCount(), System.currentTimeMillis() - start) ;
		// new InfinityThread().startNJoin();

		int totalCount = session.root().childQuery("").page(Page.ALL).find().totalCount() ;
		Debug.line(totalCount, System.currentTimeMillis() - start) ;
		
		central.close() ;
	}
	
	public void testIndexInfo() throws Exception {
		Central central = session.central();
		central.newReader().info(new InfoHandler<Void>() {

			@Override
			public Void view(IndexReader arg0, DirectoryReader dreader) throws IOException {
				Debug.line(dreader.maxDoc()) ;
				return null;
			}
		}) ;
		

	}

	
	
	
	public void testIndexSpeed() throws Exception {
//		new File("./resource/search").delete();

		
		
		
		
		Central central = session.central();
		long start = System.currentTimeMillis();
		central.newIndexer().index(new SampleIndexWriteJob(20000));
		central.close() ;
		Debug.line(System.currentTimeMillis() - start);
	}

	
	
	
	
	
	
	
	

	
	public void testWriteNode() throws Exception {
//		new File("./resource/search").delete();

		long start = System.currentTimeMillis();
		session.tranSync(new SampleWriteJob(10000));
		Debug.line(System.currentTimeMillis() - start);
		
		session.pathBy("/copy1").children().debugPrint() ;
	}
	
	
	public void testRead() throws Exception {
		session.pathBy("/copy1").children().debugPrint() ;
	}
	
	
	
}
