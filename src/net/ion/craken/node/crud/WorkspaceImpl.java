package net.ion.craken.node.crud;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.loaders.lucene.SearcherCacheStore;
import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Repository;
import net.ion.craken.tree.TreeCache;

import org.infinispan.loaders.AbstractCacheStoreConfig;


public class WorkspaceImpl extends AbstractWorkspace {


	WorkspaceImpl(Repository repository, SearcherCacheStore cacheStore, GridFilesystem gfs, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		super(repository, cacheStore, gfs, treeCache, wsName, config) ;
		treeCache.cache().start() ;
	}

	public static WorkspaceImpl create(Repository repository, SearcherCacheStore cacheStore, GridFilesystem gfs, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		return new WorkspaceImpl(repository, cacheStore, gfs, treeCache, wsName, config);
	}

}
