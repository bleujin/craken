package net.ion.craken.node.crud;

import org.infinispan.loaders.AbstractCacheStoreConfig;

import net.ion.craken.loaders.lucene.ISearcherCacheStoreConfig;
import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Repository;
import net.ion.craken.tree.TreeCache;
import net.ion.nsearcher.config.Central;


public class WorkspaceImpl extends AbstractWorkspace {


	WorkspaceImpl(Repository repository, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		super(repository, treeCache, wsName, config) ;
		treeCache.start();
	}

	public static WorkspaceImpl create(Repository repository, TreeCache treeCache, String wsName, AbstractCacheStoreConfig config) {
		return new WorkspaceImpl(repository, treeCache, wsName, config);
	}

}
