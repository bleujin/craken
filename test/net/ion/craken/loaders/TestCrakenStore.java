package net.ion.craken.loaders;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;

public class TestCrakenStore extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
		r.createWorkspace("test", WorkspaceConfigBuilder.directory("./resource/store/test")) ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		r.shutdown() ;
	}
	
	public void testWrite() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").refTo("self", "/bleujin") ;
				return null;
			}
		}) ;
	}
	
	public void testRead() throws Exception {
		ReadNode found = session.pathBy("/bleujin");
		found.debugPrint();
		found.ref("self").debugPrint();
	}
	
	
}
