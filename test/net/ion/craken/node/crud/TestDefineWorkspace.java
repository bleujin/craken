package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.store.CrakenWorkspaceConfigBuilder;

public class TestDefineWorkspace extends TestCase {

	public void testDuplDefine() throws Exception {
		Craken r = Craken.inmemoryCreateWithTest(); // predefine
		try {
			r.createWorkspace("test", CrakenWorkspaceConfigBuilder.singleDir("")); // dupl
			fail();
		} catch (IllegalArgumentException expect) {
		}
		ReadSession session = r.login("test");
		r.shutdown();

	}
	
	public void testNotStarted() throws Exception {
		Craken r = Craken.inmemoryCreateWithTest();
		ReadSession session = r.login("test") ;
		
		assertEquals(true, r.isStarted()) ;
		r.shutdown() ;
		assertEquals(false, r.isStarted()) ;
	}
}
