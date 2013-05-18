package net.ion.craken.node.crud;

import net.ion.craken.node.AbstractWorkspace;
import net.ion.craken.node.Repository;
import net.ion.craken.tree.TreeCache;

import org.infinispan.notifications.Listener;


public class WorkspaceImpl extends AbstractWorkspace {


	WorkspaceImpl(Repository repository, TreeCache treeCache, String wsName) {
		super(repository, treeCache, wsName) ;

		treeCache.start();
	}

	public static WorkspaceImpl create(Repository repository, TreeCache treeCache, String wsName) {
		return new WorkspaceImpl(repository, treeCache, wsName);
	}

}
