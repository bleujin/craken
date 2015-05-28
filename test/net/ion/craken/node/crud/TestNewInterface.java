package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.store.CrakenWorkspaceConfigBuilder;
import net.ion.framework.util.Debug;

public class TestNewInterface extends TestCase {

	public void testFromMemory() throws Exception {
		Repository r = Craken.inmemoryCreateWithTest();
		ReadSession session = r.login("test") ;
		
		long start = System.currentTimeMillis() ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.pathBy("/hero").property("name", "hero").property("age", 20) ;
				return null;
			}
		}).get() ;
		
		session.root().children().debugPrint();
		Debug.line(System.currentTimeMillis() - start);
		r.shutdown() ;
	}
	
	
	public void testReadFromSaved() throws Exception {
		Repository r = Craken.create().createWorkspace("search", CrakenWorkspaceConfigBuilder.singleDir("")) ;
		
		ReadSession session = r.login("search") ;

		session.root().children().debugPrint(); 
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.pathBy("/hero").property("name", "hero").property("age", 20) ;
				return null;
			}
		}).get() ;
		
		r.shutdown() ;
	}
	
	

}
