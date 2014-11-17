package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;

public class TestWorkspaceConfig extends TestCase {

	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	
	public void testReadWhenNotDefined() throws Exception {
		ReadSession session = r.login("notdefined") ;
		session.root().children().debugPrint(); 
	}
	

	public void testCreateWorkspace() throws Exception {
		r.createWorkspace("test", WorkspaceConfigBuilder.directory("./resource/store/test")) ;
		ReadSession session = r.login("test");
		
		session.tran(TransactionJobs.HelloBleujin) ;
	}
	
//	public void testWorkspaceNames() throws Exception {
//		r.defineWorkspace("search") ;
//		r.defineWorkspace("temp") ;
//		
//		Debug.line(r.workspaceNames()) ;
//		
//	}
//	
	public void testWorkspaceConfig() throws Exception {
		r.defineWorkspace("search") ;
		
		ReadSession session = r.login("search") ;
		Workspace workspace = session.workspace() ;
		Debug.line(workspace.cache().getCacheConfiguration()) ; 
	}
}
