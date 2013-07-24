package net.ion.craken.node.crud;

import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Repository;
import net.ion.craken.tree.TreeCache;

import org.infinispan.loaders.AbstractCacheStoreConfig;


public class WorkspaceImpl extends AbstractWorkspace {


	WorkspaceImpl(Repository repository, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		super(repository, treeCache, wsName, config) ;
		treeCache.start();
	}

	public static WorkspaceImpl create(Repository repository, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		return new WorkspaceImpl(repository, treeCache, wsName, config);
	}

}
