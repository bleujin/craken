package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.Cache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import com.google.common.cache.CacheBuilder;

public class Craken implements Repository {

	private IExecutor executor;
	private com.google.common.cache.Cache<String, Workspace> workspaceCache = CacheBuilder.newBuilder().maximumSize(20).build();
	private DefaultCacheManager dm;
	private Map<String, Object> attrs = MapUtil.newMap();
	private final Log log = LogFactory.getLog(Repository.class);
	private String repoId;

	private Craken(DefaultCacheManager dm, String repoId) {
		this.dm = dm;
		this.repoId = repoId ;
		this.executor = new IExecutor(0, 3);
		putAttribute(ColumnParser.class.getCanonicalName(), new ColumnParser());
	}

	public static Craken create() throws IOException {
		return create(new DefaultCacheManager("./resource/config/craken-cache-config.xml"), "emanon");
	}
	
	public static Craken local() throws IOException {
		return create(new DefaultCacheManager(), "emanon");
	}
	
	public static Craken create(DefaultCacheManager dcm, String repoId){
		return new Craken(dcm, repoId);
	}
	
	public static Craken inmemoryCreateWithTest() throws CorruptIndexException, IOException {
		System.setProperty("log4j.configuration", "file:./resource/log4j.properties") ;
		Craken result = local();
		return (Craken)result.createWorkspace("test", WorkspaceConfigBuilder.indexDir(""));
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
		return workspaceCache.asMap().keySet() ;
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

	public Craken putAttribute(String key, Object value) {
		attrs.put(key, value);
		return this;
	}

	private boolean started;
	public synchronized Repository start() {
		if (this.started) return this;
		
		dm.start();
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				Craken.this.shutdown() ;
			}
		}) ;
		
		started = true ;
		log.info(memberId() +" started") ;
		return this;
	}
	
	
	public Repository shutdown() {
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
	
	public ReadSession login(String wsname) throws IOException {
		return login(Credential.EMANON, wsname, null);
	}

	public ReadSession login(String wsname, Analyzer queryAnalyzer) throws IOException {
		return login(Credential.EMANON, wsname, queryAnalyzer);
	}

	public ReadSession login(Credential credential, final String wsName, Analyzer queryAnalyzer) throws IOException {
		if (! this.started) this.start() ;
		Workspace found = workspaceCache.getIfPresent(wsName) ;
		if (found == null) throw new IllegalArgumentException("not found workspace : " + wsName) ;

		return new ReadSessionImpl(credential, found, ObjectUtil.coalesce(queryAnalyzer, found.central().searchConfig().queryAnalyzer()));
	}

	public synchronized Repository createWorkspace(String wsName) throws IOException {
		return createWorkspace(wsName, OldFileConfigBuilder.directory("")) ;
	}
	
	public synchronized Repository createWorkspace(String wsName, WorkspaceConfigBuilder wconfig) throws IOException {
		if (workspaceCache.getIfPresent(wsName) != null) throw new IllegalArgumentException("already defined workspace : " + wsName) ;
		
		wconfig.build(dm, wsName) ;
		Cache<PropertyId, PropertyValue> cache = dm.getCache(wsName) ;
		workspaceCache.put(wsName, wconfig.createWorkspace(this, cache.getAdvancedCache()));

		log.info("Workspace[" + wsName + "] defined");
		return this ;
	}

	public boolean isStarted() {
		return this.started ;
	}

	public void stop() {
		shutdown() ;
	}

}
