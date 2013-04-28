package net.ion.craken.node.ref;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestPathBy extends TestBaseCrud {

	
	public void testNotFoundPath() throws Exception {
		
		assertEquals(true, ! session.exists("/bleujin")) ;
		
		try {
			assertEquals(true, session.pathBy("/bleujin") != null) ;
			fail() ;
		} catch(IllegalArgumentException expect){}
		
	}
	
	public void testInWriteSession() throws Exception {
		assertEquals(true, ! session.exists("/bleujin")) ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				assertEquals(true, wsession.pathBy("/bleujin") != null) ;
				return null;
			}
		}) ;
		
	}
}
