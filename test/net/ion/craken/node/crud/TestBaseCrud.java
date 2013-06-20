package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;

public class TestBaseCrud extends TestCase {

	protected RepositoryImpl r ;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = RepositoryImpl.testSingle() ;
		r.start() ;
		this.session = r.testLogin("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	

}
