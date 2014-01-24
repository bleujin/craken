package net.ion.craken.listener;

import java.util.List;
import java.util.Map;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.ListUtil;

@Listener
public class CDDMListener implements WorkspaceListener {

	private List<CDDHandler> ls = ListUtil.newList() ;
	private ReadSession rsession ;
	

	public CDDMListener(ReadSession rsession) {
		this.rsession = rsession ;
	}

	@Override
	public void registered(Workspace workspace) {
		
	}

	@Override
	public void unRegistered(Workspace workspace) {
		
	}
	
	public void add(CDDHandler listener){
		ls.add(listener) ;
	}
	
	public void remove(CDDHandler listener){
		ls.remove(listener) ;
	}
	
	
	@CacheEntryModified
	public void modified(CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event){
		if (event.isPre()) return ;
		if (event.getKey().getType().isStructure()) return ;
		if (! event.isOriginLocal()) return ;
		
		Fqn fqn = event.getKey().getFqn();
		for (CDDHandler listener : ls) {
			String fqnPattern = listener.pathPattern() ;
			if (! fqn.isPattern(fqnPattern)) continue ;
			
			Map<String, String> resolveMap = fqn.resolve(fqnPattern);
			TransactionJob<Void> nextTran = listener.nextTran(resolveMap, event);
			if (nextTran == null || nextTran == TransactionJob.BLANK) continue ;
			rsession.tran(nextTran) ;
		}
	}

}
