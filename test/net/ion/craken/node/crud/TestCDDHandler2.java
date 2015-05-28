package net.ion.craken.node.crud;

import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDModifyHandler;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.store.CrakenWorkspaceConfigBuilder;
import net.ion.framework.util.Debug;

public class TestCDDHandler2 extends TestCase {

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

	public void testDefineOtherWorkspace() throws Exception {
		r.createWorkspace("other", CrakenWorkspaceConfigBuilder.singleDir("")) ;
		ReadSession osession = r.login("other") ;
		
		assertEquals("test", session.workspace().wsName());
		assertEquals("other", osession.workspace().wsName());
	}
	
	public void testWriteOtherWorkspace() throws Exception {
		r.createWorkspace("other", CrakenWorkspaceConfigBuilder.singleDir("")) ;
		final ReadSession osession = r.login("other") ;

		session.workspace().cddm().add(new CDDModifyHandler("/rooms/{roomId}/messages/{msgId}") {
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				final String roomId = resolveMap.get("roomId");
				final String msgId = resolveMap.get("msgId");

				return new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession owsession) throws Exception {
						final WriteChildren members = owsession.pathBy("/rooms/" + roomId + "/members").children();
						return osession.tran(new TransactionJob<Void>(){
							public Void handle(WriteSession wsession) throws Exception {

								for (WriteNode member : members) {
									wsession.pathBy(member.fqn()).child("notify/" + msgId);
								}
								return null ;
							}
							
						}).get() ;
					}
				};
			}
		});

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/rooms/123/members/bleujin").property("name", "bleujin");
				wsession.pathBy("/rooms/123/members/ryun").property("name", "ryunhee");
				wsession.pathBy("/rooms/123/members/hero").property("name", "hero");

				wsession.pathBy("/rooms/123/messages/abcdefg").property("content", "hello! everyone");
				return null;
			}
		});

		session.workspace().cddm().await();

		osession.pathBy("/rooms/123/members/bleujin").children().debugPrint(); 
		
		
		assertEquals(true, osession.exists("/rooms/123/members/bleujin/notify/abcdefg"));
		assertEquals(true, osession.exists("/rooms/123/members/ryun/notify/abcdefg"));
		assertEquals(true, osession.exists("/rooms/123/members/hero/notify/abcdefg"));
	}

	
	public void testEviction() throws Exception {
		// default 20000
		for (int o = 0; o < 4; o++) {
			final int fo = o ;
			session.tran(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					for (int i = 0; i < 21000; i++) {
						wsession.pathBy("/idx/" + (fo * i)).property("val", i) ;
					} 
					return null;
				}
			}) ;
			
			Debug.line(session.pathBy("/idx").children().offset(25000).count(), session.workspace().cache().keySet().size()) ;
			
		}
	}
	
	
	public void testApplidChildWhenRemoveSelf() throws Exception {
		
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/dept/{deptid}/{userid}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> map, CDDModifiedEvent cddmodifiedevent) {
				// TODO Auto-generated method stub
				return null;
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> map, CDDRemovedEvent cddremovedevent) {
				final String deptId = map.get("deptid") ;
				Debug.line(deptId, session.ghostBy("/dept/" + deptId));
				session.ghostBy("/dept/" + deptId).children().debugPrint(); 
				return null;
			}
		}) ;
		
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/dept/dev/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/dept/dev/hero").property("name", "hero") ;
				wsession.pathBy("/dept/dev/jin").property("name", "jin") ;
				return null;
			}
		}) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/dept").removeSelf() ;
				return null;
			}
		}) ;
		
//		session.pathBy("/dept/dev/bleujin").debugPrint(); 
		
	}
}
