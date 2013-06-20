package net.ion.craken.node.crud;

import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Repository;
import net.ion.craken.tree.TreeCache;
import net.ion.nsearcher.config.Central;


public class WorkspaceImpl extends AbstractWorkspace {


	WorkspaceImpl(Repository repository, Central central, TreeCache treeCache, String wsName) {
		super(repository, central, treeCache, wsName) ;
		treeCache.start();
	}

	public static WorkspaceImpl create(Repository repository, Central central, TreeCache treeCache, String wsName) {
		return new WorkspaceImpl(repository, central, treeCache, wsName);
	}

}
