package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

import net.ion.craken.loaders.lucene.SearcherCacheStore;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.Debug;

import org.apache.lucene.index.IndexReader;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.loaders.CacheLoaderManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.annotation.ViewChanged;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.ViewChangedEvent;

@Listener(sync = false)
public class RepositoryListener {

	private IExecutor executor ;
	public RepositoryListener(IExecutor executor) {
		this.executor = executor ;
	}

	@ViewChanged
	public void viewChanged(ViewChangedEvent event){
		Debug.line(event) ;
	}
	
	@CacheStarted
	public void startedCache(CacheStartedEvent event) throws IOException{
		if (event.getCacheName().endsWith(".node")){
			final Cache<TreeNodeKey, Map<PropertyId, PropertyValue>> cache = event.getCacheManager().getCache(event.getCacheName());
			
			EmbeddedCacheManager dm = event.getCacheManager();
			final String cacheName = event.getCacheName() ;
			SearcherCacheStore cacheStore = (SearcherCacheStore) dm.getCache(cacheName).getAdvancedCache().getComponentRegistry().getComponent(CacheLoaderManager.class).getCacheStore();
			final long lastmodified = IndexReader.lastModified(cacheStore.central().dir());

			executor.schedule(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					final AtomicHashMap<PropertyId, PropertyValue> map = new AtomicHashMap<PropertyId, PropertyValue>();
					map.put(PropertyId.normal("lastmodified"), PropertyValue.createPrimitive(lastmodified)) ;
					cache.put(TreeNodeKey.fromString("#/start/" + cacheName), map) ;
					return null;
				}
			}, 1, TimeUnit.SECONDS) ;
		}
	}
	

	@CacheStopped
	public void stoppedCache(CacheStoppedEvent event){
//		Debug.line(event) ;
	}
	
	@CacheEntryModified
	public void entryModified(CacheEntryModifiedEvent<TreeNodeKey, Map<PropertyId, PropertyValue>> event){
//		if (event.isOriginLocal()) return ;
		final TreeNodeKey key = event.getKey();
		if (! key.getType().isSystem()) return ;
		if (event.isPre()) return ;
		
		
		Map<PropertyId, PropertyValue> map = event.getValue();
		
		Debug.line(event.getCache().getName(), map, event) ;
	}
	
	
}

class MyStatus {
	private long started = 0L ;
	private long lastModified = 0L ;
	private String cacheName ;
	private String mermberName ;
	
	public MyStatus started(long started){
		this.started = started ;
		return this ;
	}
	
	public MyStatus lastModified(long lastModified){
		this.lastModified = lastModified ; 
		return this ;
	}
	
	public MyStatus cacheName(String cacheName){
		this.cacheName = cacheName ;
		return this ;
	}
	
}
