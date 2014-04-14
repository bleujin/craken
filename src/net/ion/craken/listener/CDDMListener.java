package net.ion.craken.listener;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.WriteSessionImpl;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

@Listener
public class CDDMListener implements WorkspaceListener {

	private List<CDDHandler> ls = ListUtil.newList() ;
	private ReadSession rsession ;
	private IExecutor executor;
	private Future<Void> lastFuture;
	

	public CDDMListener(ReadSession rsession) {
		this.rsession = rsession ;
		this.executor = rsession.workspace().repository().executor() ;
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
	
	public void await() throws InterruptedException, ExecutionException{
		if(lastFuture != null) lastFuture.get() ;
	}
	
	@CacheEntryRemoved
	public void deleted(final CacheEntryRemovedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) throws Exception{
		if (! event.isPre()) return ;
		if (event.getKey().getType().isStructure()) return ;
		if (! event.isOriginLocal()) return ;

		final Fqn fqn = event.getKey().getFqn();
		for (final CDDHandler listener : ls) {
			String fqnPattern = listener.pathPattern() ;
			if (! fqn.isPattern(fqnPattern)) continue ;
			final Map<String, String> resolveMap = fqn.resolve(fqnPattern);
		
			if (AsyncCDDHandler.class.isInstance(listener)){
				this.lastFuture = executor.submitTask(applyDelete(resolveMap, listener, event)) ;
			} else {
				applyDelete(resolveMap, listener, event).call();
			}
		}
	}
	
	
	@CacheEntryModified
	public void modified(final CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) throws Exception{
		
		if (event.isPre()) return ;
		if (event.getKey().getType().isStructure()) return ;
		if (! event.isOriginLocal()) return ;

		final Fqn fqn = event.getKey().getFqn();
		for (final CDDHandler listener : ls) {

			final String fqnPattern = listener.pathPattern() ;
			if (! fqn.isPattern(fqnPattern)) continue;
			final Map<String, String> resolveMap = fqn.resolve(fqnPattern);

			if (AsyncCDDHandler.class.isInstance(listener)){
				this.lastFuture = executor.submitTask(applyModify(resolveMap, listener, event)) ;
			} else {
				applyModify(resolveMap, listener, event).call() ;
			}
			
//			rsession.tranSync(nextTran);
		}
	}

	private Callable<Void> applyDelete(final Map<String, String> resolveMap, final CDDHandler listener, final CacheEntryRemovedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event){
		return new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				TransactionJob<Void> nextTran = listener.deleted(resolveMap, event);
				if (nextTran == null || nextTran == TransactionJob.BLANK) return null ;
				
				WriteSession tsession = new WriteSessionImpl(rsession, rsession.workspace());
				rsession.workspace().tran(executor.getService(), tsession, nextTran, new TranExceptionHandler() {
					@Override
					public void handle(WriteSession tsession, TransactionJob tjob, Throwable ex) {
						Debug.line(tjob);
						ex.printStackTrace(); 
					}
				}).get() ;
				return null;
			}
		} ;
	}
	
	
	private Callable<Void> applyModify(final Map<String, String> resolveMap, final CDDHandler chandler, final CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event){
		return new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				TransactionJob<Void> nextTran = chandler.modified(resolveMap, event);
				if (nextTran == null || nextTran == TransactionJob.BLANK) return null ;
				
				WriteSession tsession = new WriteSessionImpl(rsession, rsession.workspace());
				rsession.workspace().tran(executor.getService(), tsession, nextTran, new TranExceptionHandler(){
					@Override
					public void handle(WriteSession tsession, TransactionJob tjob, Throwable ex) {
						Debug.line(tjob);
						ex.printStackTrace(); 
					}
				}).get();
				return null ;
			}
		} ;
	}

}
