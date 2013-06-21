package net.ion.craken.loaders.lucene;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.problem.store.SampleWriteJob;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;


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
	
	// 30sec per 20k(create, about 700 over per sec)
	// 18sec per 20k(load, about 1k lower per sec)
	public void testLuceneDirStore() throws Exception {
		r.defineWorkspace("test", ISearcherCacheStoreConfig.create().location("./resource/local").maxEntries(10).chunkSize(1024 * 1024 * 10)) ;
		
		r.start() ;
		
		this.session = r.testLogin("test") ;
		long start = System.currentTimeMillis() ;
		session.tran(new SampleWriteJob(200)).get() ;
		
		Debug.line(System.currentTimeMillis() - start) ;
	}
	
	public void testFind() throws Exception {
		r.defineWorkspace("test", ISearcherCacheStoreConfig.create().location("./resource/local").maxEntries(10).chunkSize(1024 * 1024 * 10)) ;

		r.start() ;
		
		this.session = r.testLogin("test") ;
		long start = System.currentTimeMillis() ;
		for (int i : ListUtil.rangeNum(20)) {
			Debug.line(session.pathBy("/" + RandomUtil.nextInt(200)).toMap()) ;
		}
		Debug.line(System.currentTimeMillis() - start) ;
		
	}

}
