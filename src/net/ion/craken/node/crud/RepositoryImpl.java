package net.ion.craken.node.crud ;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.loaders.lucene.CentralCacheStore;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.loaders.lucene.SearcherCacheStore;
import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Credential;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeCacheFactory;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.config.Central;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.loaders.CacheStore;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.manager.DefaultCacheManager;

public class RepositoryImpl implements Repository{
	
	private IExecutor executor ;
	private Map<String, AbstractWorkspace> wss = MapUtil.newCaseInsensitiveMap() ;
	private Map<String, CentralCacheStoreConfig> configs = MapUtil.newCaseInsensitiveMap() ;
	private DefaultCacheManager dm;
	private Map<String, Object> attrs = MapUtil.newMap() ;
	private Logger log = LogBroker.getLogger(Repository.class) ;
	private RepositoryListener listener;
	
	public RepositoryImpl(DefaultCacheManager dm){
		this.dm = dm ;
		this.dm.defineConfiguration(SYSTEM_CACHE, new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_ASYNC).eviction().maxEntries(1000).build()) ;
		this.executor = new IExecutor(0, 3) ;
		this.listener = new RepositoryListener(this);
		this.dm.addListener(listener) ;
		this.dm.getCache(SYSTEM_CACHE).addListener(listener) ;
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
	
	
	public String memberName(){
		return dm.getAddress().toString() ;
	}
	
	public Set<String> workspaceNames(){
		return wss.keySet() ;
	}
	
	public long lastSyncModified() throws IOException{
		long lastSycnModifiedInAllCache = 0L ;
		for(AbstractWorkspace ws : wss.values()){
			final CacheLoaderManager component = ws.getCache().cache().getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class);
			if (component == null) return 0L ;
			CacheStore store = component.getCacheStore();
			SearcherCacheStore cacheStore = (SearcherCacheStore) store ;
			lastSycnModifiedInAllCache = Math.max(cacheStore.lastSyncModified(), lastSycnModifiedInAllCache) ;
		}
		return lastSycnModifiedInAllCache ;
	}

	public void lastSyncModified(long lastSyncModified) throws IOException{
		for(AbstractWorkspace ws : wss.values()){
			final CacheLoaderManager component = ws.getCache().cache().getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class);
			if (component == null) return ;
			CacheStore store = component.getCacheStore();
			if (store == null || (! (store instanceof SearcherCacheStore))) continue ;
			SearcherCacheStore cacheStore = (SearcherCacheStore) store ;
			cacheStore.lastSyncModified(lastSyncModified) ;
		}
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
		try {
			for (String wsName : configs.keySet()) {
				loadWorkspce(wsName);
			}
			dm.start();
		} catch(IOException ex){
			throw new IllegalStateException(ex) ;
		}
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
			final TreeCache treeCache = TreeCacheFactory.createTreeCache(this, dm, wsName);
			GridFilesystem gfs = new GridFilesystem(dm.<String, byte[]>getCache(wsName + ".blobdata")) ;
			
			final AbstractWorkspace newWorkspace = WorkspaceImpl.create(this, gfs, treeCache, wsName, configs.get(wsName));

			newWorkspace.pathNode(IndexWriteConfig.Default, Fqn.ROOT) ;
			wss.put(wsName, newWorkspace) ;
			
			return wss.get(wsName) ;
		}
	}

	
	public Central central(String wsName){
		SearcherCacheStore cacheStore = (SearcherCacheStore) dm.getCache(wsName + ".node").getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore();
		if (cacheStore == null) throw new IllegalArgumentException("not defined workspace") ;

		return cacheStore.central() ;
	}


	public Repository defineWorkspace(String wsName, CentralCacheStoreConfig config) {
		configs.put(wsName, config) ;
		
		defineConfig(wsName + ".node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.eviction().maxEntries(config.maxNodeEntry())
				.transaction().syncCommitPhase(true).syncRollbackPhase(true)
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new CentralCacheStore()).addProperty("location", config.location())
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		
		
		defineConfig(wsName + ".blobdata",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC)
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty(config.Location, config.location())
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;

//		defineConfig(wsName + ".blobmeta",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC)
//				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
//				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty(config.Location, config.location())
//				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
//			
		
		log.info(wsName + " created") ;
		return this ;
	}

	public Repository defineWorkspaceForTest(String wsName, CentralCacheStoreConfig config) {
		configs.put(wsName, config) ;
		
		defineConfig(wsName + ".node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.eviction().maxEntries(config.maxNodeEntry())
				.transaction().syncCommitPhase(true).syncRollbackPhase(true)
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new CentralCacheStore()).addProperty("location", "")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		
		log.info(wsName + " created") ;
		return this ;
	}


	public RepositoryListener listener(){
		return listener ;
	}
	
}
