package net.ion.craken.node.crud;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Repository;
import net.ion.craken.tree.TreeCache;

import org.infinispan.loaders.AbstractCacheStoreConfig;


public class WorkspaceImpl extends AbstractWorkspace {


	WorkspaceImpl(Repository repository, GridFilesystem gfs, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		super(repository, gfs, treeCache, wsName, config) ;
		treeCache.cache().start() ;
	}

	public static WorkspaceImpl create(Repository repository, GridFilesystem gfs, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		return new WorkspaceImpl(repository, gfs, treeCache, wsName, config);
	}

}
