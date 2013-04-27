package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;

public class TestReadNode extends TestBaseCrud {

	
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


	public void testProperty() throws Exception {
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").value()) ;
		assertEquals(20, session.pathBy("/bleujin").property("age").value()) ;

		assertEquals("hero", session.pathBy("/hero").property("name").value()) ;
		assertEquals(30L, session.pathBy("/hero").property("age").value()) ;
	}
	
	
	public void testKeys() throws Exception {
		assertEquals(2, session.pathBy("/bleujin").keys().size()) ;
	}
	
	public void testDataSize() throws Exception {
		assertEquals(2, session.pathBy("/bleujin").dataSize()) ;
	}


	
	
	
	
}
