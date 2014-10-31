package net.ion.craken.node.crud;

import java.util.concurrent.Future;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestWriteInc  extends TestBaseCrud {

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("idx", 20).child("address").property("city", "seoul") ;
				return null;
			}
		}).get() ;
	}
	
	
	public void testInc() throws Exception {
		Future<Long> result = session.tran(new TransactionJob<Long>(){
			@Override
			public Long handle(WriteSession wsession) throws Exception {
				return wsession.pathBy("/bleujin").increase("idx").asLong(0) ;
			}
		}) ;
		
		Debug.line(result.get());
		Debug.line(session.pathBy("/bleujin").property("idx")) ;
		
	}
	

}
