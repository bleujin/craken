package net.ion.craken.node.problem.distribute;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.loaders.lucene.ISearcherCacheStoreConfig;
import net.ion.craken.node.crud.RepositoryImpl;

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
	private ExecutorService es;

	public void setUp() {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
		.defaultClusteredBuilder()
		.transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
		.build();
		
		this.repository = RepositoryImpl.create(gconfig);
		
		String wsName = "test";
		ISearcherCacheStoreConfig config = ISearcherCacheStoreConfig.createDefault() ;
		this.dm = repository.dm();
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
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				repository.shutdown() ;
			}
		}) ;
		this.es = Executors.newCachedThreadPool();
	}
	
	public void tearDown() {
		es.shutdown() ;
		dir.close() ;
		repository.shutdown() ;
	}
	
	
}
