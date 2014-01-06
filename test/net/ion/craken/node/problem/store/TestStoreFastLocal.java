package net.ion.craken.node.problem.store;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.lucene.cachestore.LuceneCacheLoader;
import org.infinispan.lucene.cachestore.LuceneCacheLoaderConfig;

public class TestStoreFastLocal extends TestCase {

	
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
	
	
	public void testLuceneDirStore() throws Exception {
		r.defineConfig("test.node", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.loaders().addLoader()
					.cacheLoader(new LuceneCacheLoader())
					.addProperty(LuceneCacheLoaderConfig.LOCATION_OPTION, "./resource/lucene")
					.addProperty(LuceneCacheLoaderConfig.AUTO_CHUNK_SIZE_OPTION, "1024")
					.loaders().preload(true).shared(false).passivation(false).build()) ;
		
		this.session = r.login("test") ;
		
		
		session.tran(new SampleWriteJob(1000)).get() ;
		Debug.line("endGet") ;
	}

	// 454 sec per 100k -> oom 
	public void testAddNode() throws Exception {
		r.defineConfig("test.node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
//				.eviction().maxEntries(10000)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty("location","./resource/local")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		
		this.session = r.login("test") ;
		session.tran(new SampleWriteJob(100000)).get() ;
		Debug.line("endGet") ;
	}
	
	public void testReadNode() throws Exception {
		r.defineConfig("test.node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
				.eviction().maxEntries(10000)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location","./resource/local")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		
		this.session = r.login("test") ;
		for (int i : ListUtil.rangeNum(10)) {
//			session.root().children().where("this.id = '/m/0hxhkfn'").debugPrint() ;
			Debug.line(session.pathBy("99999").toMap()) ;
			System.out.print('.') ;
		}
		
	}
	
	public void testWrite() throws Exception {
		ReadSession rs = r.login("test");
		rs.tranSync(new SampleWriteJob(10000)) ;
		
	}
	
	public void testReadAtLocal() throws Exception {
		ReadSession rs = r.login("test");
		rs.pathBy("/copy1").children().debugPrint() ;
	}
	
}

