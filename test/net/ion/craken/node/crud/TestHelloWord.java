package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;

public class TestHelloWord extends TestCase {

	public void testHello() throws Exception {
		Repository r = RepositoryImpl.testSingle() ;
		r.start() ;
		ReadSession session = r.testLogin("mywork") ;
		
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/hello").property("greeting", "Hello World") ;
				return null;
			}
		}) ;
		
		assertEquals("Hello World", session.pathBy("/hello").property("greeting").value()) ;
		r.shutdown() ;
	}
}
