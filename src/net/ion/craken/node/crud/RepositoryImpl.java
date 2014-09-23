package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.loaders.WorkspaceConfig;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.Credential;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.ecs.xhtml.address;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.queryparser.classic.ParseException;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.config.ConfigurationException;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.CacheLoader;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Address;

import com.google.common.cache.CacheBuilder;

public class RepositoryImpl implements Repository {

	private IExecutor executor;
	private com.google.common.cache.Cache<String, Workspace> workspaceCache = CacheBuilder.newBuilder().maximumSize(20).build();
	private Map<String, WorkspaceConfig> configs = MapUtil.newCaseInsensitiveMap();
	private DefaultCacheManager dm;
	private Map<String, Object> attrs = MapUtil.newMap();
	private Logger log = LogBroker.getLogger(Repository.class);
	private String repoId;
	private ResyncListener rsyncListener;

	private RepositoryImpl(DefaultCacheManager dm, String repoId) {
		this.dm = dm;
		this.repoId = repoId ;
		this.executor = new IExecutor(0, 3);
		putAttribute(ColumnParser.class.getCanonicalName(), new ColumnParser());
	}

	public static RepositoryImpl create() {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport()
			.clusterName("craken").nodeName("emanon")
			.addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
			.build();
		return create(gconfig);
	}
	
	public static RepositoryImpl test(DefaultCacheManager dcm, String repoId){
		RepositoryImpl result = new RepositoryImpl(dcm, "emanon");
		result.resyncListener(new ResyncListener(result)) ;
		return result ;
	}
	
	public static RepositoryImpl create(String repoId) {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder()
			.transport().clusterName("craken").nodeName(repoId)
				.addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		return create(gconfig, repoId);
	}

	public static RepositoryImpl create(GlobalConfiguration gconfig) {
		if (StringUtil.isBlank(gconfig.transport().nodeName())) throw new IllegalArgumentException("not defined repoId : transport nodename") ;
		return create(gconfig, gconfig.transport().nodeName());
	}

	public static RepositoryImpl create(GlobalConfiguration gconfig, String repoId) {
		System.setProperty("log4j.configuration", "file:./resource/log4j.properties") ;
		
		Configuration config = new ConfigurationBuilder().locking().lockAcquisitionTimeout(20000).concurrencyLevel(5000).useLockStriping(false).clustering().cacheMode(CacheMode.DIST_SYNC).invocationBatching().enable().build(); // not indexable : indexing().enable().
		
		final RepositoryImpl result = new RepositoryImpl(new DefaultCacheManager(gconfig, config), repoId);

		result.resyncListener(new ResyncListener(result)) ;
		return result;
	}

	
	private void resyncListener(ResyncListener resyncListener) {
		this.rsyncListener = resyncListener ;
		dm.addListener(resyncListener) ;
	}

	public static RepositoryImpl inmemoryCreateWithTest() throws CorruptIndexException, IOException {
		System.setProperty("log4j.configuration", "file:./resource/log4j.properties") ;
		
		RepositoryImpl result = new RepositoryImpl(new DefaultCacheManager(), "emanon");
		result.resyncListener(new ResyncListener(result)) ;
		return result.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create().location(""));
	}

	public String memberId() {
		return repoId; 
	}
	
	public String addressId(){
		return ObjectUtil.coalesce(dm.getAddress(), "inmemory").toString() ;
	}
	
	
	public List<Address> memberAddress(){
		return dm.getMembers() ;
	}
	

	public Set<String> workspaceNames() {
		return workspaceCache.asMap().keySet();
	}

	// only use for test
	public DefaultCacheManager dm() {
		return dm;
	}

	public <T> T getAttribute(String key, Class<T> clz) {
		final Object result = attrs.get(key);
		if (result == null)
			throw new IllegalArgumentException(key + " not found.");
		if (clz.isInstance(result))
			return clz.cast(result);
		throw new IllegalArgumentException(key + " not found.");
	}

	public RepositoryImpl putAttribute(String key, Object value) {
		attrs.put(key, value);
		return this;
	}

	private CountDownLatch latch ;
	private boolean started;
	public synchronized RepositoryImpl start() {
		if (this.started) return this;
		
		dm.start();
		latch = new CountDownLatch(configs.size()) ;
		try {
			for (final String wsName : configs.keySet()) {
				final Workspace found = workspaceCache.get(wsName, new Callable<Workspace>() {
					public Workspace call() throws Exception {
						final Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = dm.getCache(wsName);
						if (!cache.getCacheConfiguration().invocationBatching().enabled())
							throw new ConfigurationException("Invocation batching not enabled in current configuration! Please enable it.");
						final Workspace created = configs.get(wsName).createWorkspace(RepositoryImpl.this, cache, wsName).start() ;

						return created;
					}
				});
			}
			log.info(memberId() +" maked workspace") ;
			this.started = true ;
		} catch (ExecutionException ex) {
			throw new IllegalStateException(ex.getMessage());
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				RepositoryImpl.this.shutdown() ;
			}
		}) ;
		
		if (dm.getAddress() == null) {
			rsyncListener.inmomory() ;
		} 
		
		try {
			latch.await() ;
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		log.info(memberId() +" started") ;
		return this;
	}
	
	
	void release(){
		latch.countDown() ;
	}
	

	public RepositoryImpl shutdown() {
		if (!started) return this ;
		
		for (Workspace ws : workspaceCache.asMap().values()) {
			ws.close();
		}
		workspaceCache.cleanUp(); 
		configs.clear(); 
		
		executor.awaitUnInterupt(500, TimeUnit.MILLISECONDS);
		executor.shutdown();
		
		dm.stop();
		
		log.info(memberId() +" shutdowned") ;
		this.started = false ;
		return this;
	}

	public IExecutor executor() {
		return executor;
	}
	
	public Logger logger(){
		return log ;
	}
	
	Workspace findWorkspace(String wsName) throws ExecutionException{
		final Workspace found = workspaceCache.get(wsName, new Callable<Workspace>() {
			public Workspace call() throws Exception {
				return null ;
			}
		});
		if (found == null) throw new IllegalArgumentException("not found workspace") ;
		return found ;
	}

	public ReadSession login(String wsname) throws IOException {
		return login(Credential.EMANON, wsname, null);
	}

	public ReadSessionImpl login(String wsname, Analyzer queryAnalyzer) throws IOException {
		return login(Credential.EMANON, wsname, queryAnalyzer);
	}

	public ReadSessionImpl login(Credential credential, final String wsName, Analyzer queryAnalyzer) throws IOException {
		try {
			if (! this.started) this.start() ;

			final Workspace found = findWorkspace(wsName) ;
			return new ReadSessionImpl(credential, found, ObjectUtil.coalesce(queryAnalyzer, found.central().searchConfig().queryAnalyzer()));
		} catch (ExecutionException ex) {
			throw new IOException(ex);
		}

	}

	public RepositoryImpl defineWorkspace(String wsName, WorkspaceConfig config) throws IOException {
		if (configs.containsKey(wsName))
			throw new IllegalStateException("already define workspace : " + wsName);
		configs.put(wsName, config);

		dm.defineConfiguration(wsName,config.build());

		dm.defineConfiguration(wsName + ".blob", new ConfigurationBuilder()
				.clustering().cacheMode(CacheMode.DIST_SYNC).locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.clustering().hash().numOwners(2)
				.eviction().maxEntries(100)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FileCacheStore())
				.addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		dm.defineConfiguration(wsName + ".logmeta", FastFileCacheStore.fastStoreConfig(CacheMode.REPL_SYNC, config.location(), 1000));
		dm.defineConfiguration(wsName + ".log", FastFileCacheStore.fileStoreConfig(CacheMode.DIST_SYNC, config.location(), 100, 2));

		log.info("Workspace[" + wsName + ", DIST] defined");
		return this;
	}

	public RepositoryImpl defineWorkspaceForTest(String wsName, WorkspaceConfig config) throws IOException {
		if (configs.containsKey(wsName))
			throw new IllegalStateException("already define workspace : " + wsName);
		configs.put(wsName, config);

		dm.defineConfiguration(wsName, config.buildLocal());

		dm.defineConfiguration(wsName + ".blob", new ConfigurationBuilder()
			.eviction().maxEntries(100)
			.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FileCacheStore())
			.addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());

		log.info("Workspace[" + wsName + ", LOCAL] defined");
		return this;
	}

	public boolean isStarted() {
		return this.started ;
	}

}
