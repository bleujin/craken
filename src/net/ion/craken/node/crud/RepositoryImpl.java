package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import net.ion.craken.loaders.AStoreConfiguration;
import net.ion.craken.loaders.CrakenStoreConfigurationBuilder;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.crud.impl.OldWorkspace;
import net.ion.craken.node.crud.store.SingleFileConfigBuilder;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.Directory;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.EvictionConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import com.google.common.cache.CacheBuilder;

@Deprecated
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
		return (RepositoryImpl)result.defineWorkspace("test");
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
//		dm.<String, StringBuilder> getCache("craken-log").stop();
		
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
	
	public Log getLogger(){
		return log ;
	}
	
	Workspace findWorkspace(final String wsName) throws ExecutionException{
		final Workspace found = workspaceCache.get(wsName, new Callable<Workspace>() {
			public Workspace call() throws Exception {
				Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = dm.getCache(wsName) ;
				List<StoreConfiguration> stores = cache.getAdvancedCache().getCacheConfiguration().persistence().stores();
				return new OldWorkspace(RepositoryImpl.this, cache, wsName, stores.size() == 0 ? CentralConfig.newRam().build() : ((AStoreConfiguration)stores.get(0)).central()) ;
			}
		});
		if (found == null) throw new IllegalArgumentException("not found workspace") ;
		return found ;
	}

	public ReadSession login(String wsname) throws IOException {
		return login(Credential.EMANON, wsname, null);
	}

	public ReadSession login(String wsname, Analyzer queryAnalyzer) throws IOException {
		return login(Credential.EMANON, wsname, queryAnalyzer);
	}

	public ReadSession login(Credential credential, final String wsName, Analyzer queryAnalyzer) throws IOException {
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
		Configuration maked = makeConfig(2000, CacheMode.LOCAL);
		dm.defineConfiguration(wsName, maked);

		definedWorkspace.add(wsName) ;
		log.info("Workspace[" + wsName + ", " + maked.clustering().cacheModeString() + "] defined");
		return this;
	}

	public RepositoryImpl createWorkspace(String wsName, WorkspaceConfigBuilder wconfig) {
		if (definedWorkspace.contains(wsName)) throw new IllegalArgumentException("already defined workspace : " + wsName) ;
		wconfig.init(dm, wsName) ;
		Configuration maked = makeConfig(wconfig.maxEntry(), wconfig.cacheMode());
		dm.defineConfiguration(wsName, maked);

		definedWorkspace.add(wsName) ;
		log.info("Workspace[" + wsName + ", " + maked.clustering().cacheModeString() + "] defined");
		return this ;
	}

	public boolean isStarted() {
		return this.started ;
	}

	private Configuration makeConfig(int maxEntry, CacheMode cmode){
		EvictionConfigurationBuilder builder = new ConfigurationBuilder().read(dm.getDefaultCacheConfiguration())
			.transaction().transactionMode(TransactionMode.TRANSACTIONAL)
			.invocationBatching().enable()
			.persistence().addStore(CrakenStoreConfigurationBuilder.class).maxEntries(maxEntry).fetchPersistentState(true).preload(false).shared(false).purgeOnStartup(false).ignoreModifications(false)
			.async().enabled(false).flushLockTimeout(20000).shutdownTimeout(1000).modificationQueueSize(1000).threadPoolSize(5)
			.eviction().maxEntries(maxEntry) ; // .eviction().expiration().lifespan(10, TimeUnit.SECONDS) ;
			builder.clustering().cacheMode(cmode) ;
		
		return builder.build() ;
	}

}
