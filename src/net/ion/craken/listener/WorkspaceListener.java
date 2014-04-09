package net.ion.craken.listener;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;

public interface WorkspaceListener {

	public void registered(Workspace workspace) ;
	public void unRegistered(Workspace workspace) ;
	
//	public boolean isSupported(TreeNodeKey nodeKey) ;
//	public ReadSession readSession() ;
//	public void modified(CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) throws Exception ;
}
