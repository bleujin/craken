package net.ion.craken.node.ref;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;

public class TestBaseRef extends TestCase {

	protected RepositoryImpl r ;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = RepositoryImpl.testSingle() ;
		this.session = r.testLogin("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	
}
