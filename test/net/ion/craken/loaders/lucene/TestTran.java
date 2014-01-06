package net.ion.craken.loaders.lucene;

import net.ion.craken.loaders.neo.NeoWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import junit.framework.TestCase;

public class TestTran extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest();
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		r.shutdown();
	}

	
	public void testDelete() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").removeSelf() ;
				return null;
			}
		}) ;
	}

}
