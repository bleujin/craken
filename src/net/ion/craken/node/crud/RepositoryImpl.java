package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import net.ion.craken.loaders.AStoreConfiguration;
import net.ion.craken.loaders.CrakenStoreConfigurationBuilder;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EvictionConfigurationBuilder;
import org.infinispan.configuration.cache.ExpirationConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import com.google.common.cache.CacheBuilder;

public class RepositoryImpl implements Repository {

	private IExecutor executor;
	private com.google.common.cache.Cache<String, Workspace> workspaceCache = CacheBuilder.newBuilder().maximumSize(20).build();
	private DefaultCacheManager dm;
	private Map<String, Object> attrs = MapUtil.newMap();
	private final Log log = LogFactory.getLog(Repository.class);
	private String repoId;
	private Set<String> definedWorkspace = SetUtil.newSet() ;

	private RepositoryImpl(DefaultCacheManager dm, String repoId) {
		this.dm = dm;
		this.repoId = repoId ;
		this.executor = new IExecutor(0, 3);
		putAttribute(ColumnParser.class.getCanonicalName(), new ColumnParser());
	}

	public static RepositoryImpl create() throws IOException {
		return create(new DefaultCacheManager("./resource/config/craken-cache-config.xml"), "emanon");
	}
	
	public static RepositoryImpl create(DefaultCacheManager dcm, String repoId){
		return new RepositoryImpl(dcm, repoId);
	}
	
	public static RepositoryImpl inmemoryCreateWithTest() throws CorruptIndexException, IOException {
		System.setProperty("log4j.configuration", "file:./resource/log4j.properties") ;
		RepositoryImpl result = create(new DefaultCacheManager(), "emanon");
		return result.defineWorkspace("test");
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
		return definedWorkspace ;
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

	private boolean started;
	public synchronized RepositoryImpl start() {
		if (this.started) return this;
		
		dm.start();
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				RepositoryImpl.this.shutdown() ;
			}
		}) ;
		
		started = true ;
		log.info(memberId() +" started") ;
		return this;
	}
	
	
	public RepositoryImpl shutdown() {
		if (!started) return this ;
		
		for (Workspace ws : workspaceCache.asMap().values()) {
			ws.close();
		}
		workspaceCache.cleanUp(); 
		dm.<String, StringBuilder> getCache("craken-log").stop();
		dm.<String, StringBuilder> getCache("craken-blob").stop();
		
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
	
	public Log logger(){
		return log ;
	}
	
	Workspace findWorkspace(final String wsName) throws ExecutionException{
		final Workspace found = workspaceCache.get(wsName, new Callable<Workspace>() {
			public Workspace call() throws Exception {
				Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = dm.getCache(wsName) ;
				List<StoreConfiguration> stores = cache.getAdvancedCache().getCacheConfiguration().persistence().stores();
				return new Workspace(RepositoryImpl.this, cache, wsName, stores.size() == 0 ? CentralConfig.newRam().build() : ((AStoreConfiguration)stores.get(0)).central()) ;
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
			if (! definedWorkspace.contains(wsName)) {
				definedWorkspace.add(wsName) ;
			}

			final Workspace found = findWorkspace(wsName) ;
			return new ReadSessionImpl(credential, found, ObjectUtil.coalesce(queryAnalyzer, found.central().searchConfig().queryAnalyzer()));
		} catch (ExecutionException ex) {
			throw new IOException(ex);
		}

	}

	public RepositoryImpl defineWorkspace(String wsName) throws IOException {
		if (definedWorkspace.contains(wsName)) throw new IllegalArgumentException("already defined workspace : " + wsName) ; 
		Configuration maked = makeConfig();
		dm.defineConfiguration(wsName, maked);

		definedWorkspace.add(wsName) ;
		log.info("Workspace[" + wsName + ", " + maked.clustering().cacheModeString() + "] defined");
		return this;
	}

	public RepositoryImpl createWorkspace(String wsName, WorkspaceConfigBuilder wconfig) {
		if (definedWorkspace.contains(wsName)) throw new IllegalArgumentException("already defined workspace : " + wsName) ;
		wconfig.init(dm, wsName) ;
		Configuration maked = makeConfig();
		dm.defineConfiguration(wsName, maked);

		definedWorkspace.add(wsName) ;
		log.info("Workspace[" + wsName + ", " + maked.clustering().cacheModeString() + "] defined");
		return this ;
	}

	public boolean isStarted() {
		return this.started ;
	}

	private Configuration makeConfig(){
		EvictionConfigurationBuilder builder = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration())
				.transaction().transactionMode(TransactionMode.TRANSACTIONAL)
				.invocationBatching().enable()
				.persistence().addStore(CrakenStoreConfigurationBuilder.class).maxEntries(20000).fetchPersistentState(true).preload(false).shared(false).purgeOnStartup(false).ignoreModifications(false)
				.async().enabled(false).flushLockTimeout(20000).shutdownTimeout(1000).modificationQueueSize(1000).threadPoolSize(5)
				.eviction().maxEntries(20000) ; // .eviction().expiration().lifespan(10, TimeUnit.SECONDS) ;
			if (true){
				builder.clustering().cacheMode(dm.getDefaultCacheConfiguration().clustering().cacheMode()) ;
			}
		
		return builder.build() ;
	}

}
