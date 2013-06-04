package net.ion.craken.node.crud;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.ion.craken.io.GridFile;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.ColumnParserImpl;
import net.ion.craken.node.search.RepositorySearch;
import net.ion.craken.node.search.RepositorySearchImpl;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeCacheFactory;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

@Listener
public class RepositoryImpl implements Repository{
	
	private IExecutor executor = new IExecutor(0, 3) ;
	private Map<String, AbstractWorkspace> wss = MapUtil.newCaseInsensitiveMap() ;
	private DefaultCacheManager dm;
	private Map<String, Object> attrs = MapUtil.newMap() ;

	public RepositoryImpl(DefaultCacheManager dm){
		this.dm = dm ;
		putAttribute(ColumnParser.class.getCanonicalName(), new ColumnParserImpl()) ;
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
	
	public ReadSession testLogin(String wsname) {
		return login(Credential.EMANON, wsname) ;
	}
	
	public ReadSessionImpl login(Credential credential, String wsname) {
		return new ReadSessionImpl(credential, nodeWorkspce(wsname));
	}
	
	private synchronized AbstractWorkspace nodeWorkspce(String wsname){
		if (wss.containsKey(wsname)){
			return wss.get(wsname) ;
		} else {
			final AbstractWorkspace created = WorkspaceImpl.create(this, treeCache(wsname), wsname);
			created.getNode("/") ;
			wss.put(wsname, created) ;
			return wss.get(wsname) ;
		}
	}


	private TreeCache<PropertyId, PropertyValue> treeCache(String cacheName) {
		return new TreeCacheFactory().createTreeCache(dm, cacheName) ;
	}

	public RepositorySearch forSearch() {
		final RepositorySearchImpl result = new RepositorySearchImpl(this, dm);
		return result;
	}


	
	@CacheEntryModified
	public void entryModified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap> e) {
		if (e.isPre())
			return;
		
		if (e.getKey().getContents() == Type.DATA) {
			Debug.line(e.getKey(), e.getValue().entrySet()) ;
		}
	}


	
	
	
}
