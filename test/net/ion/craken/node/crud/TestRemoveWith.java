package net.ion.craken.node.crud;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.TouchedRow;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectId;

public class TestRemoveWith extends TestBaseCrud {

	public void testRemove() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/bleujin/address").property("name", "seoul") ;
				return null;
			}
		}) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").stringValue()) ;
		assertEquals(true, session.exists("/bleujin")) ;
		
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").removeSelf() ;
				return null;
			}
		}) ;
		
		assertEquals(false, session.exists("/bleujin")) ;
	}
	
	public void testRemoveTwice() throws Exception {
		
		for (int i = 0; i < 3; i++) {
			session.tranSync(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/bleujin").property("name", "bleujin") ;
					return null;
				}
			}) ;
			assertEquals("bleujin", session.pathBy("/bleujin").property("name").stringValue()) ;
			assertEquals(true, session.exists("/bleujin")) ;
			
			session.tranSync(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/bleujin").removeSelf() ;
					assertEquals(false, wsession.workspace().cache().containsKey(Fqn.fromString("/bleujin").dataKey())) ;

					return null;
				}
			}) ;
			assertEquals(false, session.exists("/bleujin")) ;
		}
	}
	
	
	public void testRemoveAfter() throws Exception {
		assertEquals(true, session.exists("/")) ;
		assertEquals(true, session.exists("/")) ;

//		session.tranSync(new SampleWriteJob(20));
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 20; i++) {
					wsession.createBy("/" + i).property("idx", i) ;
				}
				return null;
			}
		}) ;
		
	 	assertEquals(20, session.root().children().toList().size()) ;
	 	assertEquals(true, session.exists("/")) ;
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root().removeChildren() ;
				return null;
			}
		}) ;
		
	 	assertEquals(0, session.root().children().toList().size()) ;
	 	assertEquals(true, session.exists("/")) ;

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i : ListUtil.rangeNum(20)) {
					final WriteNode wnode = wsession.pathBy("/bleujin/" + i);
					wnode.property("key", "val") ;
				}
				return null;
			}
		}) ;

		assertEquals(true, session.exists("/")) ;
		
	 	assertEquals(20, session.pathBy("/bleujin").children().toList().size()) ;
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root().removeChildren() ;
				return null;
			}
		}) ;
	 	assertEquals(0, session.root().children().toList().size()) ;
	}
	
	
	public void testRemovedChildrenWhenRemoveSelf() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d/e/f") ;
				return null;
			}
		}) ;
		
		assertEquals(true, session.exists("/a")) ;
		assertEquals(true, session.exists("/a/b")) ;
		assertEquals(true, session.exists("/a/b/c")) ;
		assertEquals(true, session.exists("/a/b/c/d")) ;
		assertEquals(true, session.exists("/a/b/c/d/e")) ;
		assertEquals(true, session.exists("/a/b/c/d/e/f")) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d").removeSelf() ;
				return null;
			}
		}) ;

		assertEquals(true, session.exists("/a")) ;
		assertEquals(true, session.exists("/a/b")) ;
		assertEquals(true, session.exists("/a/b/c")) ;
		assertEquals(false, session.exists("/a/b/c/d")) ;
		assertEquals(false, session.exists("/a/b/c/d/e")) ;
		assertEquals(false, session.exists("/a/b/c/d/e/f")) ;
	}
	
	
	public void testRemoveChildBut() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d/e/f") ;
				return null;
			}
		}) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				boolean removed = wsession.pathBy("/a/b/c").removeChild("d/e") ;
				assertFalse(removed); // find removed target in parent, and ...
				return null;
			}
		}) ;
	}
	
	
	public void testTouchedWhenRemoveSelf() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d/e/f") ;
				return null;
			}
		}) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d").removeSelf() ;
				
				List<TouchedRow> removed = wsession.touched(Touch.REMOVE);
				assertEquals(1, removed.size());
				return null;
			}
		}) ;
	}
	
	public void testTouchedWhenRemoveChildren() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d/e/f") ;
				return null;
			}
		}) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d").removeChildren() ;
				
				List<TouchedRow> removed = wsession.touched(Touch.REMOVECHILDREN);

				assertEquals(1, removed.size());
				return null;
			}
		}) ;
	}
	
	public void testTouchedWhenRemoveChildren2() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d/e/f") ;
				return null;
			}
		}) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d").removeChildren() ;
				
				List<TouchedRow> removed = wsession.touched(Touch.REMOVE); // not found
				assertEquals(0, removed.size());
				return null;
			}
		}) ;
	}
	
	
	public void xtestWhenAssert() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b/c/d/e/f") ;
				assertEquals(1, 2);
				return null;
			}
		}) ;
	}
	
	
	public void testRemoveConcurrent() throws Exception {
		final String key = new ObjectId().toString();
		
		r.login("test").workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/thoth/{taskid}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				return null;
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				return null;
			}
		});
		r.login("test").workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/thoth/{taskid}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				return null;
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				return null;
			}
		});
		
		ExecutorService es = Executors.newCachedThreadPool() ;
		
		r.login("test").tran(new TransactionJob<Void>(){
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/thoth/" + key).property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		es.submit(new Callable<Void>(){
			public Void call() throws Exception {
				session.tran(new TransactionJob<Void>(){
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/thoth/" + key).removeSelf() ;
						return null;
					}
					
				}) ;
				return null;
			}
		}).get() ;
		
		
		r.login("test").ghostBy("/thoth").children().debugPrint();
		Debug.line();
	}
	
}
