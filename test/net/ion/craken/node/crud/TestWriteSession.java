package net.ion.craken.node.crud;

import java.util.concurrent.Future;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;

public class TestWriteSession extends TestBaseCrud {

	
	public void testTran() throws Exception {
		assertEquals(false, session.exists("/test")) ;
		
		Future<Void> future = session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession tsession) {
				WriteNode node = tsession.pathBy("/test") ;
				node.property("name", "bleujin") ;
				
				return null;
			}
		});
		future.get() ;
		
		assertEquals(true, session.exists("/test")) ;
		ReadNode found = session.pathBy("/test");
		assertEquals("bleujin", found.property("name").value()) ;
	}
	
	
	
	public void testPathByInTran() throws Exception {

		assertEquals(false, session.root().hasChild("/bleujin")) ;
		try {
			assertEquals(true, session.pathBy("/bleujin") != null) ;  
		} catch(IllegalArgumentException expect){}
//		assertEquals(true, session.root().child("/bleujin") != null) ;

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession tsession) {
				assertEquals(false, tsession.root().hasChild("/bleujin")) ; // create 
				assertEquals(true, tsession.pathBy("/bleujin") != null) ;
				assertEquals(true, tsession.root().hasChild("/bleujin")) ; // created
				assertEquals(true, tsession.root().child("/bleujin") != null) ;

				return null;
			}
		}).get();
		
	}
	
	
	public void testContinueUnit() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0 ; i < 10 ; i++) {
					wsession.pathBy("/bleujin/" + i).property("name", "bleujin").property("index", i) ;
					if ((i % 2) == 0) {
						wsession.continueUnit() ;
					}
				}
				return null;
			}
		}) ;
		
		session.pathBy("/bleujin", true).children().debugPrint() ;
		Thread.sleep(1000) ;
		session.pathBy("/bleujin", true).children().debugPrint() ;
		
	}

	
	public void testIgnoreIndex() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.ignoreIndex("name") ;
				for (int i = 0; i < 5; i++) {
					wsession.pathBy("/index/" + i).property("index", i).property("name", "bleujin") ;
				}
				return null;
			}
		}).get() ;
		
//		session.pathBy("/index").children().debugPrint() ;
		session.pathBy("/index").childQuery("").find().debugPrint() ;
//		Thread.sleep(100) ;
	}
	
}
