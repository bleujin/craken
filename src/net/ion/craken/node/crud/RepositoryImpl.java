package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.ion.bleujin.infinispan.CentralCacheStore;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.loaders.lucene.ISearcherCacheStore;
import net.ion.craken.loaders.lucene.ISearcherCacheStoreConfig;
import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeCacheFactory;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.SingleInstanceLockFactory;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.DefaultCacheManager;

public class RepositoryImpl implements Repository{
	
	private IExecutor executor = new IExecutor(0, 3) ;
	private Map<String, AbstractWorkspace> wss = MapUtil.newCaseInsensitiveMap() ;
	private Map<String, ISearcherCacheStoreConfig> configs = MapUtil.newCaseInsensitiveMap() ;
	private DefaultCacheManager dm;
	private Map<String, Object> attrs = MapUtil.newMap() ;
	private Logger log = LogBroker.getLogger(Repository.class) ;
	
	public RepositoryImpl(DefaultCacheManager dm){
		this.dm = dm ;
		putAttribute(ColumnParser.class.getCanonicalName(), new ColumnParser()) ;
	}
	
	public static RepositoryImpl create() {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
			.defaultClusteredBuilder()
				.transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
				.build();
		return create(gconfig) ;
	}

	public static RepositoryImpl create(GlobalConfiguration gconfig) {
		Configuration config = new ConfigurationBuilder()
		.locking().lockAcquisitionTimeout(20000).concurrencyLevel(5000).useLockStriping(false)
		.clustering().cacheMode(CacheMode.DIST_SYNC).invocationBatching().enable().build() ; // not indexable : indexing().enable().
		return new RepositoryImpl(new DefaultCacheManager(gconfig, config));
	}


	public static RepositoryImpl testSingle(){
		Configuration config = new ConfigurationBuilder().invocationBatching().enable().build() ; // not indexable : indexing().enable().
		return new RepositoryImpl(new DefaultCacheManager(config))  ;
	}
	
	
	// only use for test
	public DefaultCacheManager dm(){
		return dm ;
	}
	
	
	// only use for test 
	@Deprecated
	public Repository defineConfig(String cacheName, Configuration configuration) {
		dm.defineConfiguration(cacheName, configuration) ;
		return this ;
	}

	

	
	public <T> T getAttribute(String key, Class<T> clz){
		final Object result = attrs.get(key);
		if (result == null) throw new IllegalArgumentException(key + " not found.") ;
		if (clz.isInstance(result)) return clz.cast(result) ;
		throw new IllegalArgumentException(key + " not found.") ;
	}
	
	
	public RepositoryImpl putAttribute(String key, Object value){
		attrs.put(key, value) ;
		return this ;
	}
	
	public void start(){
		dm.start() ;
	}
	
	
	public void shutdown() {
		for (Workspace ws : wss.values()) {
			ws.close() ;
		}
		executor.awaitUnInterupt(500, TimeUnit.MILLISECONDS) ;
		executor.shutdown() ;
		dm.stop() ;
	}

	public IExecutor executor(){
		return executor ;
	}
	
	public ReadSession login(String wsname) throws CorruptIndexException, IOException {
		final AbstractWorkspace workspace = loadWorkspce(wsname);
		Analyzer queryAnalyzer = workspace.central().searchConfig().queryAnalyzer();
		return new ReadSessionImpl(Credential.EMANON, workspace, queryAnalyzer);
	}
	
	public ReadSessionImpl login(String wsname, Analyzer queryAnalyzer) throws CorruptIndexException, IOException {
		final AbstractWorkspace workspace = loadWorkspce(wsname);
		return new ReadSessionImpl(Credential.EMANON, workspace, queryAnalyzer);
	}

	
	public ReadSessionImpl login(Credential credential, String wsname, Analyzer queryAnalyzer) throws CorruptIndexException, IOException {
		final AbstractWorkspace workspace = loadWorkspce(wsname);
		
		return new ReadSessionImpl(credential, workspace, queryAnalyzer);
	}
	
	private synchronized AbstractWorkspace loadWorkspce(String wsName) throws CorruptIndexException, IOException{
		if (wss.containsKey(wsName)){
			return wss.get(wsName) ;
		} else {
			ISearcherCacheStoreConfig config = configs.get(wsName);
			if (dm.getTransport() == null || dm.getCacheNames().size() == 1 || (!configs.containsKey(wsName))) { // when testSingle
				final Cache<Object, Object> idxCache = dm.getCache(wsName + ".idx");
				idxCache.start() ;
				
			} else {
//						dftManager.defineConfiguration(wsname + ".meta", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).addCacheLoader()
//								.cacheLoader(new FastFileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
				
//						dftManager.defineConfiguration(wsname + ".chunks", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().eviction().maxEntries(10).invocationBatching().enable().loaders().preload(true).shared(false).passivation(
//								false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
				
//						dftManager.defineConfiguration(wsname + ".locks", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).build());
				
				
//						Directory dir = new DirectoryBuilderImpl(metaCache, chunkCache, lockCache, wsname).chunkSize(1024 * 64).create(); // .chunkSize()
				
//				InfinispanDirectory dir = new InfinispanDirectory(metaCache, chunkCache, lockCache, wsname, config.chunkSize());
//				central = CentralConfig.oldFromDir(dir).indexConfigBuilder().build();
			}

			
			final AbstractWorkspace newWorkspace = WorkspaceImpl.create(this, treeCache(wsName), wsName, ObjectUtil.coalesce(configs.get(wsName), config));
			newWorkspace.getNode("/") ;
			wss.put(wsName, newWorkspace) ;
			return wss.get(wsName) ;
		}
	}


	private TreeCache<PropertyId, PropertyValue> treeCache(String cacheName) {
		return TreeCacheFactory.createTreeCache(dm, cacheName) ;
	}

	
	public Central central(String wsName){
		CentralCacheStore cacheStore = (CentralCacheStore) dm.getCache(wsName + ".node").getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore();
		return cacheStore.central() ;
	}
	
	public void defineWorkspace(String wsName, ISearcherCacheStoreConfig config) {
		configs.put(wsName, config) ;
		

		defineConfig(wsName + ".node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.eviction().maxEntries(config.maxNodeEntry())
				.transaction().syncCommitPhase(true).syncRollbackPhase(true)
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new ISearcherCacheStore()).addProperty("location", config.location())
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;

		defineConfig(wsName + ".meta", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable()
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		
		defineConfig(wsName + ".chunks", 
				new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().eviction().maxEntries(config.maxChunkEntries()).invocationBatching().enable()
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false)
				.addCacheLoader().cacheLoader(new FileCacheStore()).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());

		defineConfig(wsName + ".locks", 
				new ConfigurationBuilder()
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).build());
		
		
		defineConfig(wsName + ".blobdata",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC)
			.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
			.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty(config.Location, config.location())
			.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;

		defineConfig(wsName + ".blobmeta",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC)
			.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
			.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty(config.Location, config.location())
			.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		
		
		log.info(wsName + " created") ;
	}

	public void defineWorkspaceForTest(String wsName, ISearcherCacheStoreConfig config) {
		configs.put(wsName, config) ;
		defineConfig(wsName + ".node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
//				.eviction().maxEntries(10000)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new ISearcherCacheStore()).addProperty("location", config.location())
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		
		
		defineConfig(wsName + ".idx", 
				new ConfigurationBuilder()
				.clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).build());
		log.info(wsName + " created") ;
	}



	
	
}
