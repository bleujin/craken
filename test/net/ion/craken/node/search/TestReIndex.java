package net.ion.craken.node.search;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestReIndex extends TestBaseCrud {

	public void testReIndex() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		
		RepositorySearch rs = r.forSearch();
		ReadSearchSession ss = rs.testLogin(session.getWorkspace().wsName());
		
		assertEquals(0, ss.createRequest("").find().size()) ; 

		Future<AtomicInteger> future = ss.reIndex(ss.root());
		assertEquals(2, future.get().get()) ;
		assertEquals(2, ss.createRequest("").find().size()) ; 
	}
	
	
	
	
	
}
