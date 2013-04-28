package net.ion.craken.node.ref;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestPathBy extends TestBaseCrud {

	
	public void testInReadSession() throws Exception {
		
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
	
	public void testRefInReadSession() throws Exception {
		try {
			session.root().ref("notfound") ;
			fail() ;
		} catch(IllegalArgumentException expect){}
	}
	
	public void testRefInWriteSession() throws Exception {
			session.tranSync(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) {
					try {
						wsession.root().ref("notfound") ;
						fail() ;
					} catch(IllegalArgumentException expect){} ;
					return null;
				}
			}) ;
	}
	
	
	
}
