package net.ion.craken.node.search;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;

public class TestBaseSearch extends TestCase {

	protected RepositoryImpl r;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = RepositoryImpl.inmemoryCreateWithTest();
		r.start() ;
		
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown();
		super.tearDown();
	}
	
}
