package net.ion.bleujin.craken;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.store.CrakenWorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import junit.framework.TestCase;

public class TestCraken extends TestCase {


	private Craken craken;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.inmemoryCreateWithTest() ;
		craken.createWorkspace("test", CrakenWorkspaceConfigBuilder.sifsDir("./resource/store/index", "./resource/store/data")) ;
//		craken.createWorkspace("", WorkspaceConfigBuilder.sifs(indexPath, dataPath, blobPath)) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.stop();
		super.tearDown();
	}
	

	public void testFirst() throws Exception {
		ReadSession session = craken.login("test") ;
		
		session.tran(TransactionJobs.dummy("/bleujin", 10)) ;
		session.pathBy("/bleujin").children().debugPrint(); 
	}
}
