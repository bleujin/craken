package net.ion.craken.listener;

import net.ion.craken.node.Workspace;

public interface WorkspaceListener {

	public void registered(Workspace workspace) ;
	public void unRegistered(Workspace workspace) ;
}
