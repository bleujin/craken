package net.ion.craken.node.crud;

import net.ion.craken.node.Credential;

public class TestReadSession extends TestBaseCrud {

	public void testSessionKey() throws Exception {
		assertEquals(Credential.EMANON.accessKey(), session.credential().accessKey()) ; 
		assertEquals(true, session.credential().secretKey() == null) ; // clear after login 
	}
	
	public void testPathBy() throws Exception {
		assertEquals(false, session.root().hasChild("/bleujin")) ;
		try {
			assertEquals(true, session.pathBy("/bleujin"));
			fail() ;
		} catch(IllegalArgumentException expect){
		}
		assertEquals(false, session.root().hasChild("/bleujin")) ;
	}
	
	public void testNotFoundPathThrowIllegalException() throws Exception {
		assertEquals(false, session.root().hasChild("/notfound")) ;
		try {
			assertEquals(true, session.root().child("/notfound") != null);
			fail() ;
		} catch(IllegalArgumentException expect){
		}
		assertEquals(false, session.root().hasChild("/notfound")) ;
	}
	
	public void testRoot() throws Exception {
		assertEquals(true, session.exists("/")) ;
	}
	
	
	public void testNotFoundPath2ThrowIllegalException() throws Exception {
		assertEquals(false, session.exists("/notfound")) ;

		try {
			session.pathBy("/notfound") ;
			fail() ;
		} catch(IllegalArgumentException expect){
		}
	}

	

	
}
