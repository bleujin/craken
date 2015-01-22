package net.ion.craken.node.crud;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.PropertyValue;

public class TestReadNode extends TestBaseCrud {

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.root().child("hero").property("name", "hero").property("age", 30L) ;
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


	public void testGetPropertyValue() throws Exception {
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").value()) ;
		assertEquals(20, session.pathBy("/bleujin").property("age").value()) ;

		assertEquals("hero", session.pathBy("/hero").property("name").value()) ;
		assertEquals(30L, session.pathBy("/hero").property("age").value()) ;
	}
	
	
	public void testNodeKeys() throws Exception {
		assertEquals(2, session.pathBy("/bleujin").normalKeys().size()) ;
	}
	
	
	public void testPropertyReplaceValue() throws Exception {
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").value("replaceValue")) ;
		assertEquals("replaceValue", session.pathBy("/bleujin").property("notfound").value("replaceValue")) ;
		assertEquals("replaceValue", session.pathBy("/bleujin").property("notfound").value("replaceValue")) ;
		assertEquals(123, ((Integer)session.pathBy("/bleujin").property("notfound").value(123)).intValue()) ;
	}


	public void testChild() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin/address").property("city", "seoul").property("", "") ;
				return null;
			}
		}) ;
		
		ReadNode node = session.pathBy("/bleujin").child("address");
		assertEquals("/bleujin/address", node.fqn().toString()) ;
	}
	
	public void testChildIfNotFoundRtnEmpty() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin/address").property("city", "seoul").property("", "") ;
				return null;
			}
		}) ;
		
		ReadNode found = session.ghostBy("/bleujin/address");
		assertEquals("/bleujin/address", found.fqn().toString()) ;
	 	assertEquals(true, found.toRows("this.city").next()) ;

		
		ReadNode notfound = session.ghostBy("/bleujin/notfound");
		assertEquals("/bleujin/notfound", notfound.fqn().toString()) ;
	 	assertEquals(false, notfound.toRows("this.city").next()) ;
		
	 	
	 	assertEquals(true, notfound.property("city") == PropertyValue.NotFound) ;

	}
	
	public void testGhost() throws Exception {
		assertEquals(false, session.ghostBy("/bleujin").isGhost()) ;
		assertEquals(true, session.ghostBy("/notfound").isGhost()) ;
	}
	
	public void testChildQuery() throws Exception {
		session.root().childQuery("bleujin").find().debugPrint();
	}
	
	
	
}
