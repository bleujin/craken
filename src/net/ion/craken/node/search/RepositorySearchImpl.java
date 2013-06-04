package net.ion.craken.node.search;

import java.io.IOException;
import java.util.Map;

import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.Credential;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeCache;
import net.ion.craken.tree.TreeCacheFactory;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.MapUtil;
import net.ion.nsearcher.config.Central;

import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.loaders.file.FileCacheStore;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

@Listener
public class RepositorySearchImpl implements RepositorySearch {

	private RepositoryImpl inner;
	private DefaultCacheManager dftManager;
	private Map<String, Central> centrals = MapUtil.newCaseInsensitiveMap();
	private Map<String, WorkspaceSearch> wss = MapUtil.newCaseInsensitiveMap() ;

	public RepositorySearchImpl(RepositoryImpl repository, DefaultCacheManager dftManager) {
		this.inner = repository;
		this.dftManager = dftManager;
	}

	@Override
	public IExecutor executor() {
		return inner.executor();
	}

	@Override
	public ReadSearchSession login(Credential credential, String wsname) throws CorruptIndexException, IOException {
		Central central = null;
		synchronized (centrals) {
			if (!centrals.containsKey(wsname)) {
				if (dftManager.getTransport() == null) { // when testSingle
					final Cache<Object, Object> idxCache = dftManager.getCache(wsname + ".idx");
					idxCache.start() ;
					InfinispanDirectory dir = new InfinispanDirectory(idxCache);
					central = MyCentralConfig.create(dir).build();
					centrals.put(wsname, central);
				} else {
					dftManager.defineConfiguration(wsname + ".meta", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).addCacheLoader()
							.cacheLoader(new FastFileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());

					dftManager.defineConfiguration(wsname + ".chunks", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().eviction().maxEntries(10).invocationBatching().enable().loaders().preload(true).shared(false).passivation(
							false).addCacheLoader().cacheLoader(new FileCacheStore()).addProperty("location", "./resource/workspace").purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());

					dftManager.defineConfiguration(wsname + ".locks", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).clustering().invocationBatching().clustering().invocationBatching().enable().loaders().preload(true).shared(false).passivation(false).build());
					final Cache<Object, Object> metaCache = dftManager.getCache(wsname + ".meta");
					final Cache<Object, Object> chunkCache = dftManager.getCache(wsname + ".chunks");
					final Cache<Object, Object> lockCache = dftManager.getCache(wsname + ".locks");
					
					metaCache.start() ;
					chunkCache.start() ;
					lockCache.start() ;

					InfinispanDirectory dir = new InfinispanDirectory(metaCache, chunkCache, lockCache, wsname, 1024 * 1024 * 10);
					
					central = MyCentralConfig.create(dir).build();
					centrals.put(wsname, central);
				}
			}
			central = centrals.get(wsname);
		}

		final WorkspaceSearch workspace = loadWorkspce(wsname, central);

		return new ReadSearchSession(credential, workspace, central);
	}

	
	private synchronized WorkspaceSearch loadWorkspce(String wsname, Central central){
		if (wss.containsKey(wsname)){
			return wss.get(wsname) ;
		} else {
			final WorkspaceSearch created = WorkspaceSearch.create(this, central, treeCache(wsname + ".node"), wsname);
			created.getNode("/") ;
			wss.put(wsname, created) ;
			return wss.get(wsname) ;
		}
	}
	
	private TreeCache<PropertyId, PropertyValue> treeCache(String cacheName) {
		return new TreeCacheFactory().createTreeCache(dftManager, cacheName) ;
	}
	
	
	public <T> T getAttribute(String key, Class<T> clz) {
		return inner.getAttribute(key, clz) ;
	}
	
	public RepositoryImpl putAttribute(String key, Object value) {
		return inner.putAttribute(key, value) ;
	}

	
	public void start(){
		inner.start() ;
	}
	
	@Override
	public void shutdown() {
		inner.shutdown();
//		for (Central central : centrals.values()) {
//			IOUtil.closeQuietly(central);
//		}
	}

	@Override
	public ReadSearchSession testLogin(String wsname) throws IOException {
		return login(Credential.EMANON, wsname);
	}

	@CacheEntryModified
	public void entryModified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap> e) {
		return ;
	}
}
