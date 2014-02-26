package net.ion.craken.node.crud;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.TestCase;
import net.ion.craken.listener.AsyncCDDHandler;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

public class TestCDDHandler extends TestCase {

	
	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		r.start() ;
		this.session = r.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	

	
	
	public void testFirst() throws Exception {
		session.workspace().cddm().add(new ToNotiHandler()) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/rooms/123/members/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/rooms/123/members/ryun").property("name", "ryunhee") ;
				wsession.pathBy("/rooms/123/members/hero").property("name", "hero") ;

				wsession.pathBy("/rooms/123/messages/abcdefg").property("content", "hello! everyone") ;
				return null;
			}
		}) ;

		Thread.sleep(1000);
		
		assertEquals(true, session.exists("/rooms/123/members/bleujin/notify/abcdefg"));
		assertEquals(true, session.exists("/rooms/123/members/ryun/notify/abcdefg"));
		assertEquals(true, session.exists("/rooms/123/members/hero/notify/abcdefg"));

		assertEquals("hello! everyone", session.pathBy("/rooms/123/members/hero/notify/abcdefg").ref("message").property("content").stringValue()) ;

		PropertyValue path = session.pathBy("/rooms/123/members/hero/notify/abcdefg").property("message") ;
		session.pathBy("/rooms/123/members/hero/notify/abcdefg").toRows("message.content").debugPrint(); 
	}
	
	
	
	public void xtestSequenceSyncJob() throws Exception {
		session.workspace().cddm().add(new NextHandler()) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("content", "hello! everyone") ;
				return null;
			}
		}) ;
		
//		assertEquals(true, session.exists("/rooms/123/members/bleujin/notify/abcdefg"));
		assertEquals("hello! everyone", session.pathBy("/message").property("msg").defaultValue("empty")) ;
	}
	
	private static final String prefix = "/newpath" ;
	private static final String path = prefix + "/ryun" ;
	
	public void testSelfModify() throws Exception {
		session.workspace().cddm().add(new SelfModifyHandler());
		
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/connections/usersn").property("message", "hello");
				wsession.pathBy(path).property("message", "hello") ;
				return null;
			}
		}) ;
		
		Thread.sleep(500);
		
		assertEquals("hello", session.pathBy(path).property("message").stringValue()) ;
	}


	public class SelfModifyHandler implements AsyncCDDHandler {
		@Override
		public String pathPattern() {
			return prefix + "/{node}";
		}

		@Override
		public TransactionJob<Void> modified(Map<String, String> resolveMap, CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) {
			return new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					String name = wsession.pathBy(path).property("message").stringValue() ;
					return null;
				}
			};
		}

		@Override
		public TransactionJob<Void> deleted(Map<String, String> resolveMap, CacheEntryRemovedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) {
			return null;
		}
		
	}
}





class NextHandler implements CDDHandler {

	@Override
	public String pathPattern() {
		return "/{userId}";
	}

	@Override
	public TransactionJob<Void> modified(Map<String, String> resolveMap, CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) {
		final String userId = resolveMap.get("userId") ;
		return new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.tranId("abcdefg") ;
				String content = wsession.pathBy("/" + userId).property("content").stringValue() ;
				wsession.pathBy("/message").property("msg", content) ;
				return null;
			}
		};
	}

	@Override
	public TransactionJob<Void> deleted(Map<String, String> resolveMap, CacheEntryRemovedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) {
		return null;
	}
	
	
	
}


class ToNotiHandler implements CDDHandler{
	
	@Override
	public String pathPattern() {
		return "/rooms/{roomId}/messages/{msgId}";
	}


	@Override
	public TransactionJob<Void> modified(Map<String, String> resolveMap, CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) {
		final String roomId = resolveMap.get("roomId");
		final String msgId = resolveMap.get("msgId");
		
		return new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteChildren members = wsession.pathBy("/rooms/"+ roomId+"/members").children() ;

				for(WriteNode member : members){
					member.addChild("notify/"+msgId).refTo("message", "/rooms/" +  roomId + "/messages/" +  msgId);
				}
				return null;
			}
		} ;
	}


	@Override
	public TransactionJob<Void> deleted(Map<String, String> resolveMap, CacheEntryRemovedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) {
		return null;
	}
}

