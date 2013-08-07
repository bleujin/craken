package net.ion.craken.node.crud;

import java.util.List;
import java.util.Map;

import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

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

	@ViewChanged
	public void startedCache(ViewChangedEvent event){
		Debug.line(event) ;
	}
	

	@CacheStopped
	public void stoppedCache(CacheStoppedEvent event){
		
		List<String> list = ListUtil.newList() ;
		
		for (int i = 0; i < list.size(); i++) {
			
		}
		
		Debug.line(event) ;
	}
	
	@CacheEntryModified
	public void entryModified(CacheEntryModifiedEvent<TreeNodeKey, Map<PropertyId, PropertyValue>> event){
		System.out.print('.') ;
	}
}
