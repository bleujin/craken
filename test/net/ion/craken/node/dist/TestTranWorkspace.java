package net.ion.craken.node.dist;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TranLogManager;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;

public class TestTranWorkspace extends TestCase {

	
	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		this.session = r.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testHistoryRead() throws Exception {
		for (int i = 0; i < 5; i++) {
			session.tranSync(TransactionJobs.dummy("/bleujin", 10)) ;
		}

		TranLogManager logm = session.workspace().tranLogManager();
		String[] trans = logm.readAll();
		assertEquals(5, trans.length) ;
		
		
		assertEquals(logm.lastTran(), trans[trans.length-1]) ;
	}
	
	
	
	
	
	
}
