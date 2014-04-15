package net.ion.craken.listener;

import java.util.Iterator;
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

import net.ion.craken.node.AbstractWriteSession;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TouchedRow;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.rows.function.TocharFunction;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.WriteChildrenEach;
import net.ion.craken.node.crud.WriteChildrenIterator;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.crud.WriteSessionImpl;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.schedule.IExecutor;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

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
	
	public void fireRow(TouchedRow[] touchedRows, final AbstractWriteSession wsession, TransactionJob tjob, TranExceptionHandler ehandler) {
		
		final JobList syncJob = JobList.create() ;
		final JobList asyncJob = JobList.create() ;
		
		final CDDHandler[] handlers = ls.toArray(new CDDHandler[0]) ;
		for (TouchedRow row : touchedRows) {
			if (row.touch() == Touch.MODIFY){
				applyModify(syncJob, asyncJob, handlers, row.target(), row, wsession) ;
			} else if (row.touch() == Touch.REMOVE){
				applyDeleted(syncJob, asyncJob, handlers, row.target(), row, wsession) ;
			} else if (row.touch() == Touch.REMOVECHILDREN) {
				Map<String, Fqn> affected = row.affected() ;
				for(Fqn fqn : affected.values()){
					applyDeleted(syncJob, asyncJob, handlers, fqn, row, wsession) ;
				}
			}
			
		}
		
		try {
			new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					for (TransactionJob<Void> job : syncJob) {
						job.handle(wsession) ;
					}

					CDDMListener.this.lastFuture = executor.submitTask(new Callable<Void>() {
						@Override
						public Void call() throws Exception {
							for (TransactionJob<Void> job : asyncJob) {
								job.handle(wsession) ;
							}
							return null;
						}
					}) ;
					return null;
				}
			}.call();
		} catch (Exception e) {
			if (ehandler != null) ehandler.handle(wsession, tjob, e);
		}
	}
	
	
	private void applyModify(JobList syncJob, JobList asyncJob, CDDHandler[] handlers,  final Fqn targetFqn, TouchedRow row, final WriteSession wsession) {
		for (final CDDHandler handler : handlers) {
			if (targetFqn.isPattern(handler.pathPattern())){
				final Map<String, String> resolveMap = targetFqn.resolve(handler.pathPattern()) ;
				 if (AsyncCDDHandler.class.isInstance(handler)) asyncJob.add(handler.modified(resolveMap, row.modifyEvent())) ; 
				 else syncJob.add(handler.modified(resolveMap, row.modifyEvent()))  ;
			} 
		}
	}
	

	private void applyDeleted(JobList syncJob, JobList asyncJob, CDDHandler[] handlers,  final Fqn targetFqn, TouchedRow row, final WriteSession wsession) {
		for (final CDDHandler handler : handlers) {
			if (targetFqn.isPattern(handler.pathPattern())){
				final Map<String, String> resolveMap = targetFqn.resolve(handler.pathPattern()) ;
				 if (AsyncCDDHandler.class.isInstance(handler)) asyncJob.add(handler.deleted(resolveMap, row.deleteEvent())) ; 
				 else syncJob.add(handler.deleted(resolveMap, row.deleteEvent()))  ;
			} 
		}
	}
}


class JobList implements Iterable<TransactionJob<Void>>{
	List<TransactionJob<Void>> list = ListUtil.newList() ;
	
	public JobList add(TransactionJob<Void> job){
		if (job != null) list.add(job) ;
		return this ;
	}
	
	public static JobList create() {
		return new JobList();
	}

	public List<TransactionJob<Void>> toList(){
		return list;
	}

	@Override
	public Iterator<TransactionJob<Void>> iterator() {
		return toList().iterator();
	}
	
}
