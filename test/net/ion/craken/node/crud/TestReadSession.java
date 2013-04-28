package net.ion.craken.node.crud;

import net.ion.craken.node.Credential;
import net.ion.framework.util.Debug;

public class TestReadSession extends TestBaseCrud {

	public void testSessionKey() throws Exception {
		assertEquals(Credential.EMANON.accessKey(), session.credential().accessKey()) ; 
		assertEquals(true, session.credential().secretKey() == null) ; // clear after login 
	}
	
	public void testPathBy() throws Exception {
		assertEquals(false, session.root().hasChild("/bleujin")) ;
		assertEquals(true, session.pathBy("/bleujin") != null) ;
		assertEquals(true, session.root().child("/bleujin") != null) ;
	}
	
	public void testIsNotNull() throws Exception {
		assertEquals(true, ! session.root().hasChild("/notfound")) ;
		assertEquals(true, session.root().child("/notfound") != null); // in create
		assertEquals(true, session.root().hasChild("/notfound")) ;
	}
	
	
	

	
}
