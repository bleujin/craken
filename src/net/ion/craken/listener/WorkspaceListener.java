package net.ion.craken.listener;

import net.ion.craken.node.Workspace;

public interface WorkspaceListener {

	public void registered(Workspace workspace) ;
	public void unRegistered(Workspace workspace) ;
	
//	public boolean isSupported(TreeNodeKey nodeKey) ;
//	public ReadSession readSession() ;
//	public void modified(CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) throws Exception ;
}
