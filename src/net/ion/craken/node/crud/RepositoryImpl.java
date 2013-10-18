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
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.SetUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryparser.classic.ParseException;
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
	
	public Map<String, Long> lastSyncModified() throws IOException, ParseException{
		Map<String, Long> result = MapUtil.newMap() ;
		
		for(AbstractWorkspace ws : wss.values()){
			Long lastCommitTime = login(ws.wsName()).logManager().lastTranInfoBy() ;
			result.put(ws.wsName(), lastCommitTime) ;
		}
		return result ;
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
			SearcherCacheStore cacheStore = (SearcherCacheStore) dm.getCache(wsName + ".node").getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore();
			
//			cacheStore.gfs(this.dm, gfs) ;
			final AbstractWorkspace newWorkspace = WorkspaceImpl.create(this, cacheStore, gfs, treeCache, wsName, configs.get(wsName));

			
			newWorkspace.init() ;
			
			wss.put(wsName, newWorkspace) ;
			return wss.get(wsName) ;
		}
	}

	
//	public Central central(String wsName){
//		SearcherCacheStore cacheStore = (SearcherCacheStore) dm.getCache(wsName + ".node").getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore();
//		if (cacheStore == null) throw new IllegalArgumentException("not defined workspace") ;
//
//		return cacheStore.central() ;
//	}


	public Repository defineWorkspace(String wsName, CentralCacheStoreConfig config) {
		configs.put(wsName, config) ;
		
		defineConfig(wsName + ".node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.eviction().maxEntries(config.maxNodeEntry())
				.transaction().syncCommitPhase(true).syncRollbackPhase(true)
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new CentralCacheStore()).addProperty(config.Location, config.location())
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

		defineConfig(wsName + ".blobdata",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC)
				.locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false)
				.build()) ;

		log.info(wsName + " created") ;
		return this ;
	}


	public RepositoryListener listener(){
		return listener ;
	}
	
}

class TransactionBean {

	public static TransactionBean BLANK = createBlank() ;
	private long time ;
	private String config ;
	private Set<String> tlogs ;

	public long time(){
		return time ;
	}
	
	private static TransactionBean createBlank() {
		TransactionBean result = new TransactionBean();
		result.time = 0L ;
		result.config = IndexWriteConfig.Default.toJson().toString() ;
		result.tlogs = SetUtil.EMPTY ;
		return result;
	}

	public IndexWriteConfig iwconfig(){
		return JsonObject.fromString(config).getAsObject(IndexWriteConfig.class) ;
	}
	
	public Set<String> tlogs(){
		return tlogs ;
	}
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}

	public static TransactionBean test(long time, Set<String> tlogs) {
		TransactionBean result = new TransactionBean();
		result.time = time ;
		result.tlogs = tlogs ;
		result.config = IndexWriteConfig.Default.toJson().toString() ;
		
		return result ;
	}
}
