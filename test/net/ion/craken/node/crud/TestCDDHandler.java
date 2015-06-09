package net.ion.craken.node.crud;

import java.util.Map;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.craken.listener.AsyncCDDModifyHandler;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDModifyHandler;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectId;

public class TestCDDHandler extends TestCase {

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

	public void testFirst() throws Exception {
		session.workspace().cddm().add(new CDDModifyHandler("/rooms/{roomId}/messages/{msgId}") {
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				final String roomId = resolveMap.get("roomId");
				final String msgId = resolveMap.get("msgId");

				return new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						WriteChildren members = wsession.pathBy("/rooms/" + roomId + "/members").children();

						for (WriteNode member : members) {
							member.child("notify/" + msgId).refTo("message", "/rooms/" + roomId + "/messages/" + msgId);
						}
						return null;
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

		assertEquals(true, session.exists("/rooms/123/members/bleujin/notify/abcdefg"));
		assertEquals(true, session.exists("/rooms/123/members/ryun/notify/abcdefg"));
		assertEquals(true, session.exists("/rooms/123/members/hero/notify/abcdefg"));

		assertEquals("hello! everyone", session.pathBy("/rooms/123/members/hero/notify/abcdefg").ref("message").property("content").stringValue());

		PropertyValue path = session.pathBy("/rooms/123/members/hero/notify/abcdefg").property("message");
		session.pathBy("/rooms/123/members/hero/notify/abcdefg").toRows("message.content").debugPrint();
	}

	
	public void testModifiedEvent() throws Exception {
		session.workspace().cddm().add(new CDDModifyHandler("/{name}") {
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				assertEquals("bleujin", resolveMap.get("name"));
				assertEquals("/bleujin", event.getKey().fqnString());
				assertEquals(2, event.getValue().size());
				return null;
			}
		});
		
		session.tranSync(TransactionJobs.HelloBleujin) ;
	}
	
	
	public void testSequenceSyncJob() throws Exception {
		session.workspace().cddm().add(new CDDModifyHandler("/{userId}") {
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				final String userId = resolveMap.get("userId");
				return new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						String content = wsession.pathBy("/" + userId).property("content").stringValue();
						wsession.pathBy("/messages" + "/" + userId).property("msg", content);
						return null;
					}
				};
			}
		});

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("content", "hello! everyone");
				return null;
			}
		});

		// assertEquals(true, session.exists("/rooms/123/members/bleujin/notify/abcdefg"));
		assertEquals("hello! everyone", session.pathBy("/messages/bleujin").property("msg").defaultValue("empty"));
	}

	public <T> void testAwaitListener() throws Exception {
		session.workspace().cddm().add(new AsyncCDDModifyHandler("/{name}") {
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				return new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						Thread.sleep(1000);
						wsession.pathBy("/bleujin/modified").property("awaited", true);
						return null;
					}

					public String toString() {
						return "CDDJob[pattern:" + pathPattern();
					}
				};

			}
		});

		session.tran(TransactionJobs.HelloBleujin);
		session.workspace().cddm().await();
		assertEquals(Boolean.TRUE, session.pathBy("/bleujin/modified").property("awaited").asBoolean());
	}

	public void testWhenSelfModify() throws Exception {
		session.workspace().cddm().add(new CDDModifyHandler("/{username}") {
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				return new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						WriteNode bleujin = wsession.pathBy("/bleujin") ;
						if ("bleujin".equals(bleujin.property("name").asString())) bleujin.property("name", "modified");
						return null;
					}
				};
			}
		});

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin");
				return null;
			}
		});

		session.workspace().cddm().await();
		assertEquals("modified", session.pathBy("/bleujin").property("name").stringValue());
	}

	public void testDeadLock() throws Exception {
		session.workspace().executorService(Executors.newCachedThreadPool()) ;
		
		session.workspace().cddm().add(new CDDHandler() {
			
			@Override
		    public String pathPattern() {
		        return "/rooms/{roomId}/members/{userId}";
		    }

		    @Override
		    public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {

		        final String roomId = resolveMap.get("roomId");
		        final String userId = resolveMap.get("userId");

		        Debug.line(roomId, userId);
		        
		        return new TransactionJob<Void>() {
		            @Override
		            public Void handle(WriteSession wsession) throws Exception {
		                String randomID = new ObjectId().toString();
		                wsession.pathBy("/rooms/" + roomId + "/messages/")
		                        .child(randomID)
		                        .property("message", userId + "님이 입장하셨습니다.") ;
		                return null;
		            }
		        };
		    }

		    @Override
		    public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
		        final String roomId = resolveMap.get("roomId");
		        final String userId = resolveMap.get("userId");
		        return new TransactionJob<Void>() {
		            @Override
		            public Void handle(WriteSession wsession) throws Exception {
		                String randomID = new ObjectId().toString();

		                //will define message
		                wsession.pathBy("/rooms/" + roomId + "/messages/")
		                        .child(randomID)
		                        .property("message", userId + "님이 퇴장하셨습니다.") ;
		                return null;
		            }
		        };
		    }
		});
		
		session.tranSync(new TransactionJob<Object>() {
			@Override
			public Object handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/rooms/1/members/ryun").property("sender", "alex");
				wsession.pathBy("/rooms/1/members/alex").property("sender", "ryun");
				return null;
			}
		});
		
		session.workspace().cddm().await(); 

//		session.pathBy("/rooms/1/messages").children().debugPrint(); 
	}
}

