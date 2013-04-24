package net.ion.craken.node.crud;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;

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
		assertEquals("bleujin", session.pathBy("/bleujin").property("name")) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "newname") ;
				return null;
			}
		}).get() ;
		
		assertEquals("newname", session.pathBy("/bleujin").property("name")) ;
	}
	
	public void testUnSet() throws Exception {
		assertEquals("bleujin", session.pathBy("/bleujin").property("name")) ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").unset("name") ;
				return null;
			}
		}).get() ;
		
		assertEquals(true, session.pathBy("/bleujin").property("name") == null) ;
		assertEquals(1, session.pathBy("/bleujin").keys().size()) ;
		assertEquals(true, session.pathBy("/bleujin").keys().contains("age")) ;
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
		assertEquals("mod", found.property("name")) ;
		assertEquals(20, found.property("age")) ;
		assertEquals(20, found.property("new")) ;
	}
	
	public void testReplaceWith() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				Object beforeValue = wsession.root().child("bleujin").replace("name", "mod") ;
				assertEquals("bleujin", beforeValue) ;
				return null;
			}
		}).get() ;
		
		
	}
	
	
}
