package net.ion.craken.node.crud;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;

public class TestRefChildren extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").refTos("dept", "/dept/dev", "/dept/solution") ;
				wsession.pathBy("/dept/dev").property("name", "dev") ;
				wsession.pathBy("/dept/solution").property("name", "solution") ;
				return null;
			}
		}) ;
	}
	
	public void testView() throws Exception {
		session.pathBy("/bleujin").refChildren("dept").debugPrint();
		assertEquals(2, session.pathBy("/bleujin").refChildren("dept").count()) ;
	}
	
	public void testFilter() throws Exception {
		assertEquals(1, session.pathBy("/bleujin").refChildren("dept").eq("name", "solution").count()) ;
	}
	
	public void testSort() throws Exception {
		assertEquals("solution", session.pathBy("/bleujin").refChildren("dept").descending("name").firstNode().property("name").asString()) ;
		assertEquals("dev", session.pathBy("/bleujin").refChildren("dept").ascending("name").firstNode().property("name").asString()) ;
	}
	
	public void testWhenNotExistRef() throws Exception {
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").refTos("dept", "/dev/notfound") ;
				return null;
			}
		}) ;
		
		assertEquals(3, session.pathBy("/bleujin").refChildren("dept").count()) ;
	}
	
	public void testRefWriteChildren() throws Exception {
		int count = session.tran(new TransactionJob<Integer>(){
			@Override
			public Integer handle(WriteSession wsession) throws Exception {
				return wsession.pathBy("/bleujin").refTos("dept", "/dept/newpart").refChildren("dept").count();
			}
			
		}).get() ;
		
		
		
		assertEquals(3, count);
	}
	
	public void testWriteInWriteSession() throws Exception {
		 session.tran(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					for(WriteNode wnode : wsession.pathBy("/bleujin").refTos("dept", "/dept/newpart").refChildren("dept")){
						wnode.property("ref", "refed") ;
					} 
					return null ;
				}
		}) ;
		 
		for (ReadNode node : session.pathBy("/bleujin").refChildren("dept")) {
			assertEquals("refed", node.property("ref").asString());
		}
	}
	
	
	public void testEach() throws Exception {
		
	}
	
}
