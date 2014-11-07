package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.Debug;

public class TestCreateBy extends TestBaseCrud {

	
	public void testCreateWith() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode wnode = wsession.pathBy("/bleujin").property("name", "bleujin");
				return null;
			}
		}) ;
		
		
		session.workspace().central().newSearcher().createRequest("name:bleujin").find().debugPrint(); 
		
		
		assertEquals(1, session.root().childQuery("name:bleujin").find().totalCount()) ;

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.createBy("/bleujin").property("name", "bleujin"); // createMode -> insert Document 
				return null;
			}
		}) ;
		
		assertEquals(2, session.root().childQuery("name:bleujin").find().totalCount()) ;
	}
	
	
	public void testResetWith() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin"); 
				return null;
			}
		}) ;
		assertEquals(1, session.root().childQuery("name:bleujin").find().totalCount()) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.resetBy("/bleujin").property("age", 20); // createMode -> insert Document 
				return null;
			}
		}) ;
		
		assertEquals(0, session.root().childQuery("name:bleujin").find().totalCount()) ; // reseted
		assertEquals(1, session.root().childQuery("age:20").find().totalCount()) ; 

	}
}
