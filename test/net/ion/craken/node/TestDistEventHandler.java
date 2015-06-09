package net.ion.craken.node;

import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;

import com.sun.corba.se.impl.activation.RepositoryImpl;

public class TestDistEventHandler extends TestCase {

	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.local() ;
		r.createWorkspace("test", OldFileConfigBuilder.directory("./resource/store")) ;
		r.start();
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown();
		super.tearDown();
	}

	public void testFirst() throws Exception {
		Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = (Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>>) session.workspace().cache() ;
//		Debug.line(cache.getListeners());
		
//		assertEquals(1, cache.getListeners().size()) ;
		
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/{userId}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				Debug.line(resolveMap, event.property("time").asLong(0));
				return null;
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				Debug.line(resolveMap, event);
				return null;
			}
		}) ;

//		new InfinityThread().startNJoin(); 
		
//		session.tran(new TransactionJob<Void>() {
//			@Override
//			public Void handle(WriteSession wsession) throws Exception {
//				wsession.pathBy("/bleujin").property("name", "bleujin") ;
//				return null;
//			}
//		}) ;
		
		
		
	}

}
