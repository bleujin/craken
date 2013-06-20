package net.ion.craken.loaders.lucene;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.problem.store.SampleWriteJob;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;


public class TestISearcherCacheStore extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	// 14sec per 20k(create, about 1k over per sec)
	// 23sec per 20k(load, about 1k lower per sec)
	public void testLuceneDirStore() throws Exception {
		r.defineConfig("test.node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
//				.eviction().maxEntries(10000)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new ISearcherCacheStore()).addProperty("location","./resource/local")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;

		r.start() ;
		
		this.session = r.testLogin("test") ;
		long start = System.currentTimeMillis() ;
		session.tran(new SampleWriteJob(20000)).get() ;
		
		Debug.line(System.currentTimeMillis() - start) ;
	}
	
	public void testFind() throws Exception {
		r.defineConfig("test.node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
//				.eviction().maxEntries(10000)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new ISearcherCacheStore()).addProperty("location","./resource/local")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;

		r.start() ;
		
		this.session = r.testLogin("test") ;
		long start = System.currentTimeMillis() ;
		for (int i : ListUtil.rangeNum(20)) {
			Debug.line(session.pathBy("/" + RandomUtil.nextInt(20000)).toMap()) ;
		}
		Debug.line(System.currentTimeMillis() - start) ;
		
	}

}
