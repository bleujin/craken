package net.ion.craken.node;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import net.ion.craken.listener.CDDHandler;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.WriteChildren;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;

public class TestSync extends TestBaseCrud {

	public void testDefaultExecutorIsWithIn() throws Exception {
		session.tran(TransactionJobs.HelloBleujin); // default exeutor is WithInExecutor
		assertEquals(true, session.exists("/bleujin"));
	}

	public void testChange() throws Exception {
		ExecutorService es = new ThreadPoolExecutor(0, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), new RejectedExecutionHandler() {
			@Override
			public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
				Debug.line(r + " rejected");
			}
		});

		session.workspace().executorService(es);
		session.tran(TransactionJobs.HelloBleujin); // default exeutor is WithInExecutor
		assertEquals(false, session.exists("/bleujin"));

	}

	public void testIndex() throws Exception {
		session.workspace().cddm().add(new ToNotiHandler());

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/rooms/123/members/bleujin").property("name", "bleujin");
				wsession.pathBy("/rooms/123/members/ryun").property("name", "ryunhee");
				wsession.pathBy("/rooms/123/members/hero").property("name", "hero");

				wsession.pathBy("/rooms/123/messages/abcdefg").property("content", "hello! everyone");
				return null;
			}
		});
	}
	
	public void testTranSync() throws Exception {
		session.workspace().cddm().add(new ToNotiHandler());

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/rooms/123/members/bleujin").property("name", "bleujin");
				wsession.pathBy("/rooms/123/members/ryun").property("name", "ryunhee");
				wsession.pathBy("/rooms/123/members/hero").property("name", "hero");

				wsession.pathBy("/rooms/123/messages/abcdefg").property("content", "hello! everyone");
				return null;
			}
		});

		Thread.sleep(1000);

		assertEquals(true, session.exists("/rooms/123/members/bleujin/notify/abcdefg"));
		assertEquals(true, session.exists("/rooms/123/members/ryun/notify/abcdefg"));
		assertEquals(true, session.exists("/rooms/123/members/hero/notify/abcdefg"));

		assertEquals("hello! everyone", session.pathBy("/rooms/123/members/hero/notify/abcdefg").ref("message").property("content").stringValue());

		PropertyValue path = session.pathBy("/rooms/123/members/hero/notify/abcdefg").property("message");
		session.pathBy("/rooms/123/members/hero/notify/abcdefg").toRows("message.content").debugPrint();
	}

	class ToNotiHandler implements CDDHandler {

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
					WriteChildren members = wsession.pathBy("/rooms/" + roomId + "/members").children();

					for (WriteNode member : members) {
						member.child("notify/" + msgId).refTo("message", "/rooms/" + roomId + "/messages/" + msgId);
					}
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
