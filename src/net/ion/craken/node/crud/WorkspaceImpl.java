package net.ion.craken.node.crud;

import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Repository;
import net.ion.craken.tree.TreeCache;
import net.ion.framework.util.Debug;

import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStarted;
import org.infinispan.notifications.cachemanagerlistener.annotation.CacheStopped;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStartedEvent;
import org.infinispan.notifications.cachemanagerlistener.event.CacheStoppedEvent;


public class WorkspaceImpl extends AbstractWorkspace {


	WorkspaceImpl(Repository repository, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		super(repository, treeCache, wsName, config) ;
		treeCache.cache().start() ;
	}

	public static WorkspaceImpl create(Repository repository, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		return new WorkspaceImpl(repository, treeCache, wsName, config);
	}

}
