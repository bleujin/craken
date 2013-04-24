package net.ion.craken.node.crud;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import junit.framework.TestCase;

public class TestBaseCrud extends TestCase {

	private Repository r ;
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
