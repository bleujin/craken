package net.ion.craken.node.crud;

import java.util.concurrent.atomic.AtomicInteger;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.util.Debug;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

public class TestListener extends TestBaseCrud {

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
				wsession.root().addChild("bleujin").property("name", "bleujin");
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
	static public class DebugListener {

		private AtomicInteger aint = new AtomicInteger() ;
		
		@CacheEntryModified
		public void modified(CacheEntryModifiedEvent<TreeNodeKey, AtomicHashMap<PropertyId, PropertyValue>> e){
			if (e.isPre()) return ;
			if (e.getKey().getType() == Type.DATA && (!e.getKey().getFqn().isSystem()))  {
				aint.incrementAndGet() ;
				Debug.line("listener", e.getKey(), e.getValue().entrySet()) ;
			}
		}
		
		public int getCount(){
			return aint.get() ;
		}
	}
	
}





