package net.ion.craken.node.crud;

import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey.Type;
import net.ion.framework.util.Debug;

import org.infinispan.atomic.impl.AtomicHashMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

public class TestWorkspaceListener extends TestCase {

	private Repository r ;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = Craken.inmemoryCreateWithTest() ; // pre define "test" ;
		
//		r.defineWorkspace("test", ISearcherWorkspaceConfig.create()) ;
//		r.defineWorkspace("test2", NeoWorkspaceConfig.create()) ;
		
		r.start() ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	
	public void testAddListener() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root() ;
				return null;
			}
		}) ;

		final DebugListener listener = new DebugListener();
		session.workspace().cache().addListener(listener) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin");
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
		
		@CacheEntryCreated
		public void created(CacheEntryCreatedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> e){
			if (e.isPre()) return ;
			if (e.getKey().getType() == Type.DATA)  {
				aint.incrementAndGet() ;
			}
		}
		
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