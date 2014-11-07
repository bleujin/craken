package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;

public class TestDefineWorkspace extends TestCase {

	public void testDuplDefine() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest(); // predefine
		try {
			r.defineWorkspace("test"); // dupl
			fail();
		} catch (IllegalArgumentException expect) {
		}
		ReadSession session = r.login("test");
		r.shutdown();

	}
	
	public void testNotStarted() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		ReadSession session = r.login("test") ;
		
		assertEquals(true, r.isStarted()) ;
		r.shutdown() ;
		assertEquals(false, r.isStarted()) ;
	}
}
