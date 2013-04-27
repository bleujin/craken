package net.ion.craken.node.crud;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;

public class TestWriteNode extends TestBaseCrud {

	
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
	
	public void testId() throws Exception {
		assertEquals(true, session.pathBy("/").id() != null) ;
		assertEquals(true, session.pathBy("/bleujin").id() != null) ;
	}
	
	public void testProperty() throws Exception {
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").value()) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "newname") ;
				return null;
			}
		}).get() ;
		
		assertEquals("newname", session.pathBy("/bleujin").property("name").value()) ;
	}
	
	
	
	public void testAddChildisMerge() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("city", "seoul") ;
				return null;
			}
		}).get() ;
		
		assertEquals("seoul", session.pathBy("/bleujin").property("city").value()) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").value()) ;
		assertEquals(20, session.pathBy("/bleujin").property("age").value()) ;
		
	}
	
	
	public void testUnSet() throws Exception {
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").value()) ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").unset("name") ;
				return null;
			}
		}).get() ;
		
		assertEquals(true, session.pathBy("/bleujin").property("name").value() == null) ;
		assertEquals(1, session.pathBy("/bleujin").keys().size()) ;
		assertEquals(true, session.pathBy("/bleujin").property("age").value() != null) ;
	}
	
	
	public void testClear() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").clear() ;
				return null;
			}
		}).get() ;
		assertEquals(0, session.pathBy("/bleujin").keys().size()) ;
	}
	
	public void testReplace() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("bleujin").property("name", "mod").property("new", 20) ;
				return null;
			}
		}).get() ;
		
		final ReadNode found = session.pathBy("/bleujin");
		assertEquals("mod", found.property("name").value()) ;
		assertEquals(20, found.property("age").value()) ;
		assertEquals(20, found.property("new").value()) ;
	}
	
	public void testReplaceWith() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				PropertyValue beforeValue = wsession.root().addChild("bleujin").replace("name", "mod") ;
				assertEquals("bleujin", beforeValue.value()) ;
				return null;
			}
		}).get() ;
		
		
	}
	
	public void testPropertyIfAbsent() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				PropertyValue beforeValue = wsession.root().addChild("absent").propertyIfAbsent("key", "value") ;
				assertEquals(true, beforeValue.value() == null) ;
				return null;
			}
		}).get() ;
		
		assertEquals("value", session.pathBy("/absent").property("key").value()) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				PropertyValue beforeValue = wsession.root().addChild("absent").propertyIfAbsent("key", "mod") ; // not modified
				assertEquals("value", beforeValue.value()) ;
				return null;
			}
		}).get() ;

		assertEquals("value", session.pathBy("/absent").property("key").value()) ;

	}
	
	
	
	
	
	
}
