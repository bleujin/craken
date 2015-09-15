package net.ion.craken.listener;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import net.ion.craken.node.AbstractWriteSession;
import net.ion.craken.node.TouchedRow;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.radon.util.uriparser.URIPattern;

import org.infinispan.atomic.impl.AtomicHashMap;
import org.infinispan.notifications.cachelistener.event.CacheEntryEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.infinispan.notifications.cachelistener.event.Event.Type;

public class CDDMListener implements WorkspaceListener {

	private Map<CDDHandler, URIPattern> chandlers = MapUtil.newSyncMap() ;
	private Future<Void> lastFuture;

	public CDDMListener() {
	}

	@Override
	public void registered(Workspace workspace) {

	}

	@Override
	public void unRegistered(Workspace workspace) {

	}

	public CDDMListener add(CDDHandler listener) {
		chandlers.put(listener, new URIPattern(listener.pathPattern()));
		return this ;
	}

	public void clear() {
		chandlers.clear(); 
	}

	public void remove(CDDHandler listener) {
		chandlers.remove(listener);
	}
	
	public Map<CDDHandler, URIPattern> handlers(){
		return Collections.unmodifiableMap(chandlers) ;
	}
	

	public void await() throws InterruptedException, ExecutionException {
		if (lastFuture != null) {
			lastFuture.get();
		}
	}


	public void modifiedRow(CacheEntryEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> event) {
		if (event.getType() == Type.CACHE_ENTRY_CREATED){
			if (event.isOriginLocal()) return ;
			if (event.isOriginLocal()) return ;
		} else if (event.getType() == Type.CACHE_ENTRY_MODIFIED) {
			if (event.isPre()) return ;
			if (event.isOriginLocal()) return ;
		}
		
		for(Entry<CDDHandler, URIPattern> entry : chandlers.entrySet()){
			CDDHandler handler = entry.getKey() ;
			Fqn targetFqn = event.getKey().getFqn();
			if (targetFqn.isPattern(entry.getValue())){
				CDDModifiedEvent mevent = new CDDModifiedEvent(event.getKey(), event.getValue()).etype(event.getType());
				Map<String, String> resolveMap = targetFqn.resolve(handler.pathPattern());;
				handler.modified(resolveMap, mevent) ;
			}
		}
		
	}

	public void removedRow(CacheEntryRemovedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> event) {
		if (event.isPre()) return ;
		if (event.isOriginLocal()) return ;

		for(Entry<CDDHandler, URIPattern> entry : chandlers.entrySet()){
			CDDHandler handler = entry.getKey() ;
			Fqn targetFqn = event.getKey().getFqn();
			if (targetFqn.isPattern(entry.getValue())){
				CDDRemovedEvent mevent = new CDDRemovedEvent(event.getKey());
				Map<String, String> resolveMap = targetFqn.resolve(handler.pathPattern());;
				handler.deleted(resolveMap, mevent) ;
			}
		}
	}

	
	public void fireRow(final AbstractWriteSession wsession, TransactionJob tjob, TranExceptionHandler ehandler) {

		TouchedRow[] touchedRows = wsession.logRows().toArray(new TouchedRow[0]);

		final JobList syncJob = JobList.create();
		final JobList asyncJob = JobList.create();

		final CDDHandler[] handlers = chandlers.keySet().toArray(new CDDHandler[0]);
		for (TouchedRow row : touchedRows) {
			if (row.touch() == Touch.MODIFY) {
				applyModify(syncJob, asyncJob, handlers, row.target(), row, wsession);
			} else if (row.touch() == Touch.REMOVE) {
				applyDeleted(syncJob, asyncJob, handlers, row.target(), row, wsession);
			} else if (row.touch() == Touch.REMOVECHILDREN) {
				Map<String, Fqn> affected = row.affected();
				for (Fqn fqn : affected.values()) {
					applyDeleted(syncJob, asyncJob, handlers, fqn, row, wsession);
				}
			}
		}
		try {

			final WriteSession newSession = wsession.workspace().newWriteSession(wsession.readSession()) ;
			if (syncJob.size() > 0) {
				wsession.workspace().tran(newSession, new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						for (TransactionJob<Void> job : syncJob) {
							job.handle(newSession);
						}
						return null;
					}
				}, ehandler).get();
			}

			if (asyncJob.size() > 0) {
				this.lastFuture = wsession.workspace().tran(newSession.workspace().repository().executor().getService(), newSession, new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						for (TransactionJob<Void> job : asyncJob) {
							job.handle(newSession);
						}
						return null;
					}
				}, ehandler);
			}

		} catch (Exception e) {
			if (ehandler != null)
				ehandler.handle(wsession, tjob, e);
		}
	}

	private void applyModify(JobList syncJob, JobList asyncJob, CDDHandler[] handlers, final Fqn targetFqn, TouchedRow row, final WriteSession wsession) {
		for (final CDDHandler handler : handlers) {
			URIPattern find = chandlers.get(handler);
			if (find == null) continue ;
			if (targetFqn.isPattern(find)) {
				final Map<String, String> resolveMap = targetFqn.resolve(handler.pathPattern());
				if (AsyncCDDHandler.class.isInstance(handler))
					asyncJob.add(handler.modified(resolveMap, row.modifyEvent()));
				else
					syncJob.add(handler.modified(resolveMap, row.modifyEvent()));
			}
		}
	}

	private void applyDeleted(JobList syncJob, JobList asyncJob, CDDHandler[] handlers, final Fqn targetFqn, TouchedRow row, final WriteSession wsession) {
		for (final CDDHandler handler : handlers) {
			URIPattern find = chandlers.get(handler);
			if (find == null) continue ;
			if (targetFqn.isPattern(find)) {
				final Map<String, String> resolveMap = targetFqn.resolve(handler.pathPattern());
				if (AsyncCDDHandler.class.isInstance(handler))
					asyncJob.add(handler.deleted(resolveMap, row.deleteEvent()));
				else
					syncJob.add(handler.deleted(resolveMap, row.deleteEvent()));
			}
		}
	}


}

class JobList implements Iterable<TransactionJob<Void>> {
	List<TransactionJob<Void>> list = ListUtil.newList();

	public JobList add(TransactionJob<Void> job) {
		if (job != null)
			list.add(job);
		return this;
	}

	public int size() {
		return list.size();
	}

	public static JobList create() {
		return new JobList();
	}

	public List<TransactionJob<Void>> toList() {
		return list;
	}

	@Override
	public Iterator<TransactionJob<Void>> iterator() {
		return toList().iterator();
	}

}
