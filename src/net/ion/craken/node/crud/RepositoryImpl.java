package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.DefaultCacheManager;

public class RepositoryImpl implements Repository{
	
	private IExecutor executor = new IExecutor(0, 3) ;
	private Map<String, AbstractWorkspace> wss = MapUtil.newCaseInsensitiveMap() ;
	private DefaultCacheManager dm;
	private Map<String, Object> attrs = MapUtil.newMap() ;
	
	
	

	public RepositoryImpl(DefaultCacheManager dm){
		this.dm = dm ;
		putAttribute(ColumnParser.class.getCanonicalName(), new ColumnParser()) ;
	}
	
	public static RepositoryImpl create() {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder
			.defaultClusteredBuilder().transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		return create(gconfig) ;
	}

	public static RepositoryImpl create(GlobalConfiguration gconfig) {
		Configuration config = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).invocationBatching().enable().build() ; // not indexable : indexing().enable().
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
	
	public ReadSession testLogin(String wsname) throws CorruptIndexException, IOException {
		return login(Credential.EMANON, wsname) ;
	}
	
	public ReadSessionImpl login(Credential credential, String wsname) throws CorruptIndexException, IOException {
		
		final AbstractWorkspace workspace = loadWorkspce(wsname);
		return new ReadSessionImpl(credential, workspace);
	}
	
	private synchronized AbstractWorkspace loadWorkspce(String wsname) throws CorruptIndexException, IOException{
		if (wss.containsKey(wsname)){
			return wss.get(wsname) ;
		} else {
			Central central = null;
			if (dm.getTransport() == null) { // when testSingle
				final Cache<Object, Object> idxCache = dm.getCache(wsname + ".idx");
				idxCache.start() ;
				InfinispanDirectory dir = new InfinispanDirectory(idxCache);
				central = CentralConfig.oldFromDir(dir).build();
			} else {
//						dftManager.defineConfiguration(wsname + ".meta", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).addCacheLoader()
//								.cacheLoader(new FastFileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
				
//						dftManager.defineConfiguration(wsname + ".chunks", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().eviction().maxEntries(10).invocationBatching().enable().loaders().preload(true).shared(false).passivation(
//								false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
				
//						dftManager.defineConfiguration(wsname + ".locks", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).build());
				
				final Cache<Object, Object> metaCache = dm.getCache(wsname + ".meta");
				final Cache<Object, Object> chunkCache = dm.getCache(wsname + ".chunks");
				final Cache<Object, Object> lockCache = dm.getCache(wsname + ".locks");
				
				metaCache.start() ;
				chunkCache.start() ;
				lockCache.start() ;
				
//						Directory dir = new DirectoryBuilderImpl(metaCache, chunkCache, lockCache, wsname).chunkSize(1024 * 64).create(); // .chunkSize()
				InfinispanDirectory dir = new InfinispanDirectory(metaCache, chunkCache, lockCache, wsname, 1024 * 1024 * 10);
				
				central = CentralConfig.oldFromDir(dir).indexConfigBuilder().build();
			}

			
			final AbstractWorkspace created = WorkspaceImpl.create(this, central, treeCache(wsname), wsname);
			created.getNode("/") ;
			wss.put(wsname, created) ;
			return wss.get(wsname) ;
		}
	}


	private TreeCache<PropertyId, PropertyValue> treeCache(String cacheName) {
		return new TreeCacheFactory().createTreeCache(dm, cacheName) ;
	}


	
	
}
