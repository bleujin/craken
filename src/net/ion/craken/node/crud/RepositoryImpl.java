package net.ion.craken.node.crud;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.search.RepositorySearch;
import net.ion.craken.node.search.RepositorySearchImpl;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeCacheFactory;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
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
	private Map<String, WorkspaceImpl> wss = MapUtil.newCaseInsensitiveMap() ;
	private DefaultCacheManager dm;

	public RepositoryImpl(DefaultCacheManager dm){
		this.dm = dm ;
		dm.start() ;
	}
	
	public static RepositoryImpl create() {
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("crakensearch").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		return create(gconfig) ;
	}

	public static RepositoryImpl create(GlobalConfiguration gconfig) {
		Configuration config = new ConfigurationBuilder().invocationBatching().enable().build() ; // not indexable : indexing().enable().
		return new RepositoryImpl(new DefaultCacheManager(gconfig, config));
	}


	public static RepositoryImpl testSingle(){
		Configuration config = new ConfigurationBuilder().invocationBatching().enable().build() ; // not indexable : indexing().enable().
		return new RepositoryImpl(new DefaultCacheManager(config))  ;
	}
	
	
	private TreeCache<String, ? extends Object> treeCache(String string) {
		Cache<String, ? extends Object> cache = dm.getCache();
		return new TreeCacheFactory().createTreeCache(cache) ;
	}
	
	public void shutdown() {
		for (Workspace ws : wss.values()) {
			ws.close() ;
		}
		executor.awaitUnInterupt(100, TimeUnit.MILLISECONDS) ;
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
		return new ReadSessionImpl(credential, loadWorkspce(wsname));
	}
	
	private synchronized WorkspaceImpl loadWorkspce(String wsname){
		if (wss.containsKey(wsname)){
			return wss.get(wsname) ;
		} else {
			final WorkspaceImpl created = WorkspaceImpl.create(this, treeCache(wsname + ".node"), wsname);
			created.getNode("/") ;
			wss.put(wsname, created) ;
			return wss.get(wsname) ;
		}
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
