package net.ion.craken.node.problem.distribute;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.loaders.lucene.OldCacheStoreConfig;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.nsearcher.common.AbDocument;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.search.Searcher;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;

public class TestISearchDistribute extends TestCase {

	private InfinispanDirectory dir;
	private DefaultCacheManager dm;
	private RepositoryImpl repository;
	private ExecutorService workerPool;
	private Central central;

	public void setUp() throws Exception{
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
		.defaultClusteredBuilder()
		.transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
		.build();
		
		this.repository = RepositoryImpl.create(gconfig);
		
		String wsName = "test";
		OldCacheStoreConfig config = OldCacheStoreConfig.createDefault() ;
		this.dm = repository.dm();
		dm.defineConfiguration(wsName + ".meta", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false)
				.build());
		dm.defineConfiguration(wsName + ".chunks", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().eviction().maxEntries(config.maxChunkEntries()).invocationBatching().enable().loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new FileCacheStore()).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false)
				.build());
		
		dm.defineConfiguration(wsName + ".locks", 
				new ConfigurationBuilder()
				.clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false)
				.build());
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				repository.shutdown() ;
			}
		}) ;
		dm.getCache("test.meta").start() ;
		dm.getCache("test.chunks").start() ;
		dm.getCache("test.locks").start() ;
		dm.start() ;
		
		this.dir = new InfinispanDirectory(dm.getCache("test.meta"), dm.getCache("test.chunks"), dm.getCache("test.locks"), "testdir", 1024 * 1024);

		this.workerPool = Executors.newCachedThreadPool();
		this.central = CentralConfig.oldFromDir(dir).build() ;
		
	}
	
	public void tearDown() throws Exception{
		workerPool.shutdown() ;
		dir.close() ;
		dm.stop() ;
		repository.shutdown() ;
	}
	
	public void testReadNWrite() throws Exception {
		for (int i = 0; i < 1000; i++) {
			workerPool.submit(new CentralIndexJob(central)) ;
			workerPool.submit(new CentralSearchJob(central)) ;
			Thread.sleep(20) ;
		}
		new InfinityThread().startNJoin() ;
	}

	public void testRead() throws Exception {
		for (int i = 0; i < 1000; i++) {
//			workerPool.submit(new CentralIndexJob(central)) ;
			workerPool.submit(new CentralSearchJob(central)) ;
			Thread.sleep(100) ;
		}
		new InfinityThread().startNJoin() ;
	}

}

class CentralSearchJob implements Callable<Void>{

	private Central central ;
	public CentralSearchJob(Central central){
		this.central = central ;
	}

	@Override
	public Void call() throws Exception {
		Searcher searcher = central.newSearcher();
		Debug.debug(searcher.createRequest("").find().totalCount()) ;
		return null;
	}
}

class CentralIndexJob implements Callable<Void> {

	private Central central ;
	public CentralIndexJob(Central central){
		this.central = central ;
	}
	
	@Override
	public Void call() throws Exception {
		Indexer indexer = central.newIndexer();
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				for (int i = 0; i < 3; i++) {
					WriteDocument doc = isession.newDocument();
					doc.add(MyField.keyword("name", "bleujin")) ;
					isession.insertDocument(doc) ;
				}
				return null;
			}
		}) ;
		return null;
	}
	
}


