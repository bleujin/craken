package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.loaders.lucene.CentralCacheStore;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.Credential;
import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

import org.apache.commons.lang.builder.ToStringBuilder;
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
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.manager.DefaultCacheManager;

import com.google.common.cache.CacheBuilder;

public class RepositoryImpl implements Repository {

	private IExecutor executor;
	private com.google.common.cache.Cache<String, Workspace> workspaceMap = CacheBuilder.newBuilder().maximumSize(20).build();
	private Map<String, CentralCacheStoreConfig> configs = MapUtil.newCaseInsensitiveMap();
	private DefaultCacheManager dm;
	private Map<String, Object> attrs = MapUtil.newMap();
	private Logger log = LogBroker.getLogger(Repository.class);

	public RepositoryImpl(DefaultCacheManager dm) {
		this.dm = dm;
		this.executor = new IExecutor(0, 3);
		putAttribute(ColumnParser.class.getCanonicalName(), new ColumnParser());
	}

	public static RepositoryImpl create() {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		return create(gconfig);
	}

	public static RepositoryImpl create(GlobalConfiguration gconfig) {
		Configuration config = new ConfigurationBuilder().locking().lockAcquisitionTimeout(20000).concurrencyLevel(5000).useLockStriping(false).clustering().cacheMode(CacheMode.DIST_SYNC).invocationBatching().enable().build(); // not indexable : indexing().enable().
		final RepositoryImpl result = new RepositoryImpl(new DefaultCacheManager(gconfig, config));
		
		result.dm.defineConfiguration(SYSTEM_CACHE, new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_ASYNC).eviction().maxEntries(1000).build());
		RepositoryListener listener = new RepositoryListener(result);
		result.dm.addListener(listener);
		result.dm.getCache(SYSTEM_CACHE).addListener(listener);
		
		return result;
	}

	public static RepositoryImpl inmemoryCreateWithTest() throws CorruptIndexException, IOException {
		RepositoryImpl result = new RepositoryImpl(new DefaultCacheManager());
		return result.defineWorkspaceForTest("test", CentralCacheStoreConfig.create().location(""));
	}

	public String memberName() {
		return dm.getAddress().toString();
	}

	public Set<String> workspaceNames() {
		return workspaceMap.asMap().keySet();
	}

	public Map<String, Long> lastSyncModified() throws IOException, ExecutionException, ParseException {
		Map<String, Long> result = MapUtil.newMap();

		for (Workspace ws : workspaceMap.asMap().values()) {
			Long lastCommitTime = login(ws.wsName()).logManager().lastTranInfoBy();
			result.put(ws.wsName(), lastCommitTime);
		}
		return result;
	}

	// only use for test
	public DefaultCacheManager dm() {
		return dm;
	}

	// only use for test
	@Deprecated
	public Repository defineConfig(String cacheName, Configuration configuration) {
		dm.defineConfiguration(cacheName, configuration);
		return this;
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

	public void start() throws IOException {
		dm.start();
		 for (String wsName : configs.keySet()) {
			 login(wsName) ;
		 }
	}

	public void shutdown() {
		for (Workspace ws : workspaceMap.asMap().values()) {
			ws.close();
		}
		executor.awaitUnInterupt(500, TimeUnit.MILLISECONDS);
		executor.shutdown();
		dm.stop();
	}

	public IExecutor executor() {
		return executor;
	}

	public ReadSession login(String wsname) throws IOException {
		return login(Credential.EMANON, wsname, null);
	}

	public ReadSessionImpl login(String wsname, Analyzer queryAnalyzer) throws IOException {
		return login(Credential.EMANON, wsname, queryAnalyzer);
	}

	public ReadSessionImpl login(Credential credential, final String wsName, Analyzer queryAnalyzer) throws IOException {
		try {

			final Workspace workspace = workspaceMap.get(wsName, new Callable<Workspace>() {
				public Workspace call() throws Exception {
					final Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = dm.getCache(wsName);
					if (!cache.getCacheConfiguration().invocationBatching().enabled())
						throw new ConfigurationException("Invocation batching not enabled in current configuration! Please enable it.");
					cache.start();
					final WorkspaceImpl result = WorkspaceImpl.create(RepositoryImpl.this, cache, wsName, configs.get(wsName));
					
//					result.begin() ;
//					result.pathNode(Fqn.ROOT, true) ;
//					result.end() ;
					return result;
				}
			});

			return new ReadSessionImpl(credential, workspace, ObjectUtil.coalesce(queryAnalyzer, workspace.central().searchConfig().queryAnalyzer()));
		} catch (ExecutionException ex) {
			throw new IOException(ex);
		}

	}


	public RepositoryImpl defineWorkspace(String wsName, CentralCacheStoreConfig config) {
		if (configs.containsKey(wsName)) throw new IllegalStateException("already define workspace : " + wsName) ;
		configs.put(wsName, config);

		dm.defineConfiguration(wsName, new ConfigurationBuilder()
				.clustering().cacheMode(CacheMode.DIST_SYNC).invocationBatching().enable().clustering()
				.eviction().maxEntries(config.maxNodeEntry()).transaction().syncCommitPhase(true).syncRollbackPhase(true).locking().lockAcquisitionTimeout(config.lockTimeoutMs())
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(CentralCacheStore.blank()).addProperty(config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false)
				.build());

		dm.defineConfiguration(wsName + ".blob", new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).locking().lockAcquisitionTimeout(config.lockTimeoutMs()).loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty(
				config.Location, config.location()).purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		dm.defineConfiguration(wsName + ".logmeta", FastFileCacheStore.fastStoreConfig(CacheMode.REPL_SYNC, config.location(), 1000));
		dm.defineConfiguration(wsName + ".log", FastFileCacheStore.fileStoreConfig(CacheMode.DIST_SYNC, config.location(), 7));

		log.info("Workspace[" + wsName + ", DIST] defined");
		return this ;
	}

	public RepositoryImpl defineWorkspaceForTest(String wsName, CentralCacheStoreConfig config) throws CorruptIndexException, IOException {
		if (configs.containsKey(wsName)) throw new IllegalStateException("already define workspace : " + wsName) ;
		configs.put(wsName, config);

		dm.defineConfiguration(wsName, new ConfigurationBuilder()
			.clustering().cacheMode(CacheMode.LOCAL).invocationBatching().enable().eviction()
			.eviction().maxEntries(config.maxNodeEntry()).transaction().syncCommitPhase(true).syncRollbackPhase(true).locking().lockAcquisitionTimeout(config.lockTimeoutMs())
			.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(CentralCacheStore.blank()).addProperty(config.Location, config.location()).purgeOnStartup(true).ignoreModifications(false).fetchPersistentState(true).async().enabled(false)
			.build());

		log.info("Workspace[" + wsName + ", LOCAL] defined");
		return this ;
	}


}

class TransactionBean {

	public static TransactionBean BLANK = createBlank();
	private long time;
	private String config;
	private Set<String> tlogs;

	public long time() {
		return time;
	}

	private static TransactionBean createBlank() {
		TransactionBean result = new TransactionBean();
		result.time = 0L;
		result.config = IndexWriteConfig.Default.toJson().toString();
		result.tlogs = SetUtil.EMPTY;
		return result;
	}

	public IndexWriteConfig iwconfig() {
		return JsonObject.fromString(config).getAsObject(IndexWriteConfig.class);
	}

	public Set<String> tlogs() {
		return tlogs;
	}

	public String toString() {
		return ToStringBuilder.reflectionToString(this);
	}

	public static TransactionBean test(long time, Set<String> tlogs) {
		TransactionBean result = new TransactionBean();
		result.time = time;
		result.tlogs = tlogs;
		result.config = IndexWriteConfig.Default.toJson().toString();

		return result;
	}
}
