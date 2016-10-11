package net.ion.craken.node.crud;

import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.listener.AsyncCDDHandler;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDModifyHandler;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;

public class TestCDDMListener extends TestCase {
	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.inmemoryCreateWithTest();
		r.start();
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown();
		super.tearDown();
	}

	
	public void testSyncHandler() throws Exception {
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/{name}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				Debug.line(event.getValue().values());

				final String name = resolveMap.get("name") ;
				return new TransactionJob<Void>(){
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/" + name + "/cdd").property("modified", true) ;
						return null;
					}
				};
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				return null;
			}
		});
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals(true, session.pathBy("/bleujin/cdd").property("modified").asBoolean().booleanValue()) ;
		
	}
	
	public void testASyncHandler() throws Exception {
		session.workspace().cddm().add(new AsyncCDDHandler() {
			@Override
			public String pathPattern() {
				return "/{name}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				final String name = resolveMap.get("name") ;
				return new TransactionJob<Void>(){
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						Thread.sleep(500);
						wsession.pathBy("/" + name + "/cdd").property("modified", true) ;
						return null;
					}
				};
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				return null;
			}
		});
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
//		assertEquals(false, session.exists("/bleujin")) ;
		
		session.workspace().cddm().await(); 
		assertEquals(true, session.exists("/bleujin")) ;
		session.ghostBy("/bleujin").debugPrint(); 
	}
	
	
	public void testRemoveHandler() throws Exception {
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/{name}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				return null ;
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				final String name = resolveMap.get("name") ;
				return new TransactionJob<Void>(){
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/" + name + "/cdd").property("modified", true) ;
						return null;
					}
				};
			}
		});
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").removeSelf() ;
				return null;
			}
		}) ;
		
		assertEquals(true, session.pathBy("/bleujin/cdd").property("modified").asBoolean().booleanValue()) ;
	}
	
	
	public void testRemoveChildren() throws Exception {
		session.tran(TransactionJobs.dummy("/bleujin", 10)) ;
		
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/bleujin/{index}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				return null;
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				final String index = resolveMap.get("index") ;
				return new TransactionJob(){
					@Override
					public Object handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/removed/" + index).property("removed", true) ;
						return "";
					}
				};
			}
		});
		
		
		session.tran(new TransactionJob(){
			@Override
			public Object handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").removeChildren(); 
				return null;
			}
			
		}) ;
		
		
		assertEquals(10, session.pathBy("removed").children().count()) ;
	}
	
	
	public void testChain() throws Exception {
		session.workspace().cddm().add(new CDDModifyHandler("/{userId}") {
			@Override
			public TransactionJob<Void> modified(final Map<String, String> resolveMap, CDDModifiedEvent event) {
				return new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						String userId = resolveMap.get("userId") ;
						wsession.pathBy("/" + userId + "/address") ;
						return null;
					}
				};
			}
		});
		session.workspace().cddm().add(new CDDModifyHandler("/{userId}/{address}") {
			@Override
			public TransactionJob<Void> modified(final Map<String, String> resolveMap, CDDModifiedEvent event) {
				return new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						String userId = resolveMap.get("userId") ;
						WriteNode address = wsession.pathBy("/" + userId + "/address");
						if (address.property("city").isBlank()) address.property("city", "seoul") ;
						return null;
					}
				};
			}
		});
		
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		session.pathBy("/bleujin").children().debugPrint();
		Debug.line(session.pathBy("/bleujin/address").toMap());
		
	}
	
	
}
