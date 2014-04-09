package net.ion.craken.node.crud;

import java.util.concurrent.atomic.AtomicInteger;

import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TreeNodeKey.Type;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

public class TestWorkspaceListener extends TestBaseCrud {

	public void testAddListener() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root() ;
				return null;
			}
		}) ;

		final DebugListener listener = new DebugListener();
		session.workspace().addListener(listener) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("bleujin").property("name", "bleujin");
				return null ;
			}
		}).get() ;
		
		assertEquals(1, listener.getCount()) ;
	}
	
	public void testRemoveListener() throws Exception {
		final DebugListener listener = new DebugListener();
		session.workspace().addListener(listener) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin");
				return null ;
			}
		}).get() ;
		
		session.workspace().removeListener(listener) ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin");
				return null ;
			}
		}).get() ;
		assertEquals(1, listener.getCount()) ;

	}
	
	
	
	
	

	@Listener
	static public class DebugListener implements WorkspaceListener {

		private AtomicInteger aint = new AtomicInteger() ;
		
		@CacheEntryModified
		public void modified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> e){
			if (e.isPre()) return ;
			if (e.getKey().getType() == Type.DATA)  {
				aint.incrementAndGet() ;
				Debug.line("listener", e.getKey(), e.getValue().entrySet()) ;
			}
		}
		
		public int getCount(){
			return aint.get() ;
		}

		@Override
		public void registered(Workspace workspace) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unRegistered(Workspace workspace) {
			// TODO Auto-generated method stub
			
		}
	}
	
}