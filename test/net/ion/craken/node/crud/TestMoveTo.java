package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TransactionJobs;

public class TestMoveTo extends TestBaseCrud {

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).child("address").property("city", "seoul") ;
				wsession.pathBy("/hero").property("name", "hero").property("age", 30L) ;
				return null;
			}
		}).get() ;
	}

	public void testMove() throws Exception {
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (WriteNode wnode : wsession.root().children()){
					wnode.moveTo("/emp") ;
				}
				return null;
			}
		}) ;
		
		assertEquals(true, session.exists("/emp/hero"));
		assertEquals(true, session.exists("/emp/bleujin"));
		assertEquals(true, session.exists("/emp/bleujin/address"));
		assertEquals(true, session.exists("/emp"));
		assertEquals(false, session.exists("/bleujin"));
		
		// move / copy 시에
		// reference는 알아서 다시 설정해야 한다. 
	}
	
	public void testMoveDepth() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/emp/bleujin/1").property("name", "bleujin").property("age", 20).child("address").property("city", "seoul") ;
				return null;
			}
		}).get() ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				WriteNode moved = wsession.pathBy("/emp/bleujin/1").moveTo("/emp/hero", 1);
				assertEquals("/emp/hero/1", moved.fqn().toString());
				return null;
			}
		}).get() ;
		
		session.root().walkChildren().debugPrint();
		
		assertEquals(true, session.pathBy("/emp/hero/1/address").property("city").asString().equals("seoul"));
		assertEquals(false, session.exists("/emp/bleujin/1"));
		
	}
	
	
	public void testCopyWithChild() throws Exception {
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").refTo("friend", "/hero") ;
				for (WriteNode wnode : wsession.root().children()){
					WriteNode copied = wnode.copyTo("/emp", true) ;
					assertEquals("/emp/" + wnode.fqn().name() , copied.fqn().toString());
				}
				return null;
			}
		}) ;

		assertEquals(true, session.exists("/emp/hero"));
		assertEquals(true, session.exists("/emp/bleujin"));
		assertEquals(true, session.exists("/emp/bleujin/address"));
		assertEquals(true, session.exists("/emp"));
		assertEquals(true, session.exists("/bleujin"));

		session.root().walkChildren().debugPrint(); 
	}

	public void testCopyOnlySelf() throws Exception {
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (WriteNode wnode : wsession.root().children()){
					wnode.copyTo("/emp", false) ;
				}
				return null;
			}
		}) ;

		assertEquals(true, session.exists("/emp/hero"));
		assertEquals(true, session.exists("/emp/bleujin"));
		assertEquals(false, session.exists("/emp/bleujin/address"));
		assertEquals(true, session.exists("/emp"));
		assertEquals(true, session.exists("/bleujin"));
	}


	public void testForRemove() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/category/bleujin").property("catid", "bleujin") ;
				return null;
			}
		}) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/category/bleujin").moveTo("/removed/category") ;
				return null;
			}
		}) ;
		
		assertEquals("bleujin", session.pathBy("/removed/category/bleujin").property("catid").asString());
		assertEquals(false, session.exists("/category/bleujin"));
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/removed/category/bleujin").moveTo("/category") ;
				return null;
			}
		}) ;
		assertEquals("bleujin", session.pathBy("/category/bleujin").property("catid").asString());

	}
	
}
