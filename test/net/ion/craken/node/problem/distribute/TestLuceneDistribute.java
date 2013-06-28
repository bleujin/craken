package net.ion.craken.node.problem.distribute;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;

import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.loaders.lucene.ISearcherCacheStoreConfig;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ObjectId;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.DefaultCacheManager;

public class TestLuceneDistribute extends TestCase  {
	
	private InfinispanDirectory dir;
	private DefaultCacheManager dm;
	private ExecutorService es;

	public void setUp() {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
			.defaultClusteredBuilder()
			.transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
			.build();
		
		String wsName = "test";
		this.dm = new DefaultCacheManager(gconfig);
		ISearcherCacheStoreConfig config = ISearcherCacheStoreConfig.createDefault();
		dm.defineConfiguration(wsName + ".meta", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		dm.defineConfiguration(wsName + ".chunks", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().eviction().maxEntries(config.maxEntries()).invocationBatching().enable().loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new FileCacheStore()).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		
		dm.defineConfiguration(wsName + ".locks", 
				new ConfigurationBuilder()
		.clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).build());
		
		this.dir = new InfinispanDirectory(dm.getCache("test.meta"), dm.getCache("test.chunks"), dm.getCache("test.locks"), "testdir", 1024 * 1024);
		
		this.es = Executors.newCachedThreadPool();
	}
	
	public void tearDown() {
		es.shutdown() ;
		dir.close() ;
	}

	public void testReadNWrite() throws Exception {
		for (int i = 0; i < 1000; i++) {
			es.submit(new ReadJob(dir));
			es.submit(new WriteJob(dir));
			Thread.sleep(200);
		}

		
//		readJob.close() ;
		Thread.sleep(1000) ;
	}
	

	public void testRead() throws Exception {
		for (int i = 0; i < 100; i++) {
			es.submit(new ReadJob(dir));
			Thread.sleep(200);
		}
		
//		readJob.close() ;
		Thread.sleep(1000) ;
	}
	

	
	public void testAppendWrite() throws Exception {
		final IndexWriter iwriter = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_CURRENT), MaxFieldLength.LIMITED);
		
		for (int i = 0; i < 5000; i++) {
			final ReadJob readJob = new ReadJob(dir);
			es.submit(readJob);
			es.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					iwriter.addDocument(createDoc(new ObjectId().toString(), 1)) ;
					iwriter.commit() ;
					return null;
				}
			});
			Thread.sleep(2000);
		}
		
	}
	
	

	public static Document createDoc(String groupKey, int i){
		Document doc = new Document();
		doc.add(new Field("tranid", groupKey, Store.YES, Index.ANALYZED));
		doc.add(new NumericField("index", Store.YES, true).setIntValue(i));
		doc.add(new Field("name", "bleujin", Store.YES, Index.ANALYZED));
		return doc ;
	}
	
}



class ReadJob implements Callable<Void>{

	
	private Directory dir ;
	public ReadJob(Directory dir) {
		this.dir = dir ;
	}
	
	@Override
	public Void call() throws Exception {
		IndexReader reader = IndexReader.open(dir);
		IndexSearcher searcher = new IndexSearcher(reader);

		try {
			Debug.line(searcher.search(new MatchAllDocsQuery(), 1).totalHits) ;
		} finally {
			IOUtil.closeQuietly(reader) ;
			IOUtil.closeQuietly(searcher) ;
		}
		
		return null;
	}
	
}


class WriteJob implements Callable<Void>{
	
	private Directory dir;
	public WriteJob(Directory dir){
		this.dir = dir ;
	}
	
	@Override
	public Void call() throws Exception {
		IndexWriter iw = null ;
		try {
			iw = new IndexWriter(dir, new StandardAnalyzer(Version.LUCENE_CURRENT), MaxFieldLength.UNLIMITED);
			String key = new ObjectId().toString() ;
			for (int i = 0; i < 3 ; i++) {
				iw.addDocument(TestLuceneDistribute.createDoc(key, i));
			}
			iw.commit() ;
		} catch(Throwable e){
			if (iw != null) iw.rollback();
			e.printStackTrace() ;
		} finally {
			IOUtil.closeQuietly(iw) ;
		}
		return null;
	}
} 

