package net.ion.craken.node.search;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestReIndex extends TestBaseSearch {

	public void testReIndex() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		
		assertEquals(1, session.queryRequest("").find().size()) ; 

		Future<AtomicInteger> future = session.reIndex(session.root());
		assertEquals(2, future.get().get()) ; // root, bleujin
		assertEquals(0, session.queryRequest("").find().size()) ; 
	}
	
	
	
	
	
}
