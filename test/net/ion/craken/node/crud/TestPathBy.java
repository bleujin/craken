package net.ion.craken.node.crud;

import java.util.Map;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.MapUtil;

public class TestPathBy extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.root().addChild("hero").property("name", "hero").property("age", 30L) ;
				return null;
			}
		}).get() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().removeChildren() ;
				return null;
			}
		}).get() ;
		super.tearDown();
	}
	
	
	public void testCreateChild() throws Exception {
		ReadNode bleujin = session.root().child("bleujin");
		assertEquals("bleujin", bleujin.property("name").value()) ;
		assertEquals(20, bleujin.property("age").value()) ;
		
		ReadNode hero = session.pathBy("/hero");
		assertEquals("hero", hero.property("name").value()) ;
		assertEquals(30L, hero.property("age").value()) ;
	}
	
	
	public void testHasChild() throws Exception {
		ReadNode root = session.root();
		assertEquals(true, root.hasChild("bleujin")) ;
		assertEquals(true, root.hasChild("hero")) ;
		assertEquals(false, root.hasChild("jin")) ;
	}
	
	
	public void testGrandChild() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").addChild("novision").property("name", "novision") ;
				return null;
			}
		}).get() ;
		
		
		assertEquals(true, session.exists("/bleujin/novision")) ;
		assertEquals(true, session.root().hasChild("/bleujin/novision")) ;
		
		
		ReadNode bleujin = session.root().child("bleujin");
		assertEquals(true, bleujin != null) ;
		
		assertEquals(true, bleujin.hasChild("novision")) ;
		assertEquals(true, bleujin.hasChild("/novision")) ;
	}
	
	public void testParent() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").addChild("novision").property("name", "novision") ;
				return null;
			}
		}).get() ;
		
		assertEquals(true, session.root().parent().equals(session.root())) ; // root's parent is root
		
		ReadNode novision = session.pathBy("/bleujin/novision");
		assertEquals(true, session.pathBy("/bleujin").equals(novision.parent())) ;
		assertEquals(true, session.root().equals(novision.parent().parent())) ;
	}
	
	
	
	
	
	public void testMergeInWriteSession() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/a/b/c/1").property("name", "line") ;
				return null;
			}
		}).get() ;
		
		ReadNode child = session.pathBy("/a").child("/b").child("/c").child("1"); // check not null
		assertEquals(3, session.root().children().toList().size()) ;
	}
	
	public void testMergeChildInWriteSession() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/a/b/c/d/e/f").property("name", "line") ;
				wsession.root().addChild("/1/2/3/4/5/6").property("name", "line") ;
				return null;
			}
		}).get() ;
		
		
		Map<Fqn, ReadNode> childrenMap = MapUtil.newMap() ;
		for (ReadNode node : session.pathBy("/").children().toList()) {
			childrenMap.put(node.fqn(), node) ;
		}
		
		assertEquals(4, childrenMap.size()) ;
		assertEquals(true, childrenMap.containsKey(Fqn.fromString("/a"))) ;
		assertEquals(true, childrenMap.containsKey(Fqn.fromString("/1"))) ;
		assertEquals(true, childrenMap.containsKey(Fqn.fromString("/bleujin"))) ;
		assertEquals(true, childrenMap.containsKey(Fqn.fromString("/hero"))) ;

//		assertEquals(expected, actual) (session.exists("/a/b"), session.exists("/a"), session.exists("/a/b/c")) ;
		
	}
	
	public void testThrowExceptionPathInReadSession() throws Exception {
		try {
			session.pathBy("/notexist") ;
			fail();
		} catch(IllegalArgumentException expect){
		}
		
		session.ghostBy("/notexist") ; // ignore
	}
	
	
	public void testMergedInWriteSession() throws Exception {
		assertEquals(true, session.exists("/bleujin")) ;
		assertEquals(false, session.exists("/jin")) ;
		assertEquals(false, session.exists("/jin")) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				assertEquals(true, wsession.pathBy("/bleujin") != null) ;
				return null;
			}
		}) ;
	}
	
	
	
	public void testRemoveChild() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/a/b/c/d/e/f").property("name", "line") ;
				wsession.pathBy("/a/b/c").property("name", "c") ;
				return null;
			}
		}).get() ;
		
		assertEquals(true, session.exists("/a/b/c/d")) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				assertEquals(false, wsession.pathBy("/a/b/").removeChild("c/d/e/f")) ; // illegal
//				assertEquals(true, wsession.pathBy("/a/b/c/d/e/").removeChild("f")) ;

				assertEquals(true, wsession.pathBy("/a/b/c").removeChild("d")) ;
				
				return null;
			}
		}).get() ;
		
		assertEquals(false, session.exists("/a/b/c/d")) ; // exist child
		assertEquals("c", session.pathBy("/a/b/c").property("name").value()) ;
	}
	
	
	public void testRemoveChild2() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/a/b/c/d/e/f").property("name", "line") ;
				return null;
			}
		}).get() ;

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				assertEquals(false, wsession.pathBy("/a/b/c/d/e").removeChild("")) ;
				return null;
			}
		}).get() ;
		
		assertEquals(true, session.exists("/a/b/c/d/e")) ;

	}
	
	public void testRemoveChildren() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/a/b/c/d/e/f").property("name", "line") ;
				wsession.pathBy("/a/b/c").property("name", "c") ;
				return null;
			}
		}).get() ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/a/b/c").removeChildren() ;
				return null;
			}
		}).get() ;
		
		assertEquals(true, session.exists("/a/b/c")) ; // exclusive self
		assertEquals(false, session.exists("/a/b/c/d")) ; // removed
		assertEquals(false, session.exists("/a/b/c/d/e")) ; 
	}
	
	
	
}
