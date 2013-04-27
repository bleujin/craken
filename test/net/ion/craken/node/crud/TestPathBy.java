package net.ion.craken.node.crud;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import net.ion.bleujin.EmbedCacheTest.DebugListener;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import junit.framework.TestCase;

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
	
	
	public void testMerge() throws Exception {
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
	
	public void testMergeChild() throws Exception {
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
				boolean removed = wsession.root().removeChild("/a/b/c") ;
				assertEquals(false, removed) ; // don't remove 
				return null;
			}
		}).get() ;
		
		assertEquals(true, session.exists("/a/b/c/d")) ; // exist child
		assertEquals("c", session.pathBy("/a/b/c").property("name").value()) ;
		
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
