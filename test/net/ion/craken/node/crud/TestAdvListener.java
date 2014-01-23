package net.ion.craken.node.crud;

import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;

import net.ion.craken.listener.WorkspaceListener;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import junit.framework.TestCase;

public class TestAdvListener extends TestCase {

	
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
	
	
	
	public void testMulti() throws Exception {
		ExecutorService es = Executors.newFixedThreadPool(3) ;
		Future<Void> future = null;
		for (int i : ListUtil.rangeNum(1000)) {
			future = es.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					session.tranSync(TransactionJobs.dummy("/bleujin", 5)) ;
					return null;
				}
			}) ;
		}
		
		future.get() ;
	}
	
	
	public void testFirst() throws Exception {
		session.workspace().addListener(new ToNotiListener(session)) ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/rooms/123/members/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/rooms/123/members/ryun").property("name", "ryunhee") ;
				wsession.pathBy("/rooms/123/members/hero").property("name", "hero") ;
				return null;
			}
		}) ;
		
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/rooms/123/messages/abcdefg").property("content", "hello! everyone") ;
				return null;
			}
		}) ;
		
		
		Thread.sleep(1000);
		
		assertEquals(true, session.exists("/rooms/123/members/bleujin/notify/abcdefg"));
		assertEquals(true, session.exists("/rooms/123/members/ryun/notify/abcdefg"));
		assertEquals(true, session.exists("/rooms/123/members/hero/notify/abcdefg"));

		
		Debug.line(session.pathBy("/rooms/123/members/hero/notify/abcdefg").ref("message").property("content")) ;
		
		
		PropertyValue path = session.pathBy("/rooms/123/members/hero/notify/abcdefg").property("message") ;
		
		session.pathBy("/rooms/123/members/hero/notify/abcdefg").toRows("message.content").debugPrint(); 
	}

	
	
	@Listener
	public static class ToNotiListener {
		
		private ReadSession rsession;
		ToNotiListener(ReadSession rsession){
			this.rsession = rsession ;
		}
		
		@CacheEntryModified
		public void modified(CacheEntryModifiedEvent<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> event) throws Exception{
			if (event.isPre()) return ;
			if (event.getKey().getType().isStructure()) return ;
			
			Fqn fqn = event.getKey().getFqn();
			String fqnPattern = "/rooms/{roomId}/messages/{msgId}";
			
			if (! fqn.isPattern(fqnPattern)) return ;

			Map<String, String> resolve = fqn.resolve(fqnPattern);
			final String roomId = resolve.get("roomId");
			final String msgId = resolve.get("msgId");
			
			rsession.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					WriteChildren members = wsession.pathBy("/rooms/"+ roomId+"/members").children() ;

					for(WriteNode member : members){
						member.addChild("notify/"+msgId).refTo("message", "/rooms/" +  roomId + "/messages/" +  msgId);
					}
					return null;
				}
			}) ;
		}
	}	
}

