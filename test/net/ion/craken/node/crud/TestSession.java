package net.ion.craken.node.crud;

import net.ion.craken.node.Credential;

public class TestSession extends TestBaseCrud {

	public void testSessionKey() throws Exception {
		assertEquals(Credential.EMANON.accessKey(), session.credential().accessKey()) ; 
		assertEquals(true, session.credential().secretKey() == null) ; // clear after login 
	}
}
