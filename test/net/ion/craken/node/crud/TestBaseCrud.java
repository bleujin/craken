package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.framework.util.Debug;

public class TestBaseCrud extends TestCase {

	protected Craken r ;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = Craken.inmemoryCreateWithTest() ; // pre define "test" ;
		
//		r.defineWorkspace("test", ISearcherWorkspaceConfig.create()) ;
//		r.defineWorkspace("test2", NeoWorkspaceConfig.create()) ;
		
		r.start() ;
		this.session = r.login("test") ;
		Debug.line(getClass().getCanonicalName());
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}

	
}
