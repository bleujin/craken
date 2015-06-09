package net.ion.craken.node.search;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;

public class TestBaseSearch extends TestCase {

	protected Craken r;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		this.r = Craken.inmemoryCreateWithTest();
		r.start() ;
		
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown();
		super.tearDown();
	}
	
}
