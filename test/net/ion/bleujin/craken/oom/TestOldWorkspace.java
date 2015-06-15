package net.ion.bleujin.craken.oom;

import java.io.File;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;

public class TestOldWorkspace extends TestBaseWorkspace {

	private Craken craken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.local() ;
		
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.shutdown() ;
		super.tearDown();
	}
	
	public void testConfirmOOM() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/old"));
		
		craken.createWorkspace("old", WorkspaceConfigBuilder.oldDir("./resource/old")) ;
		
		ReadSession session = craken.login("old");
		session.tran(makeJob(200000));
	}
	
	public void testIndexConfirm() throws Exception {
		craken.createWorkspace("old", OldFileConfigBuilder.directory("./resource/old")) ;
		
		ReadSession session = craken.login("old");
		Debug.line(session.root().childQuery("", true).offset(10000).find().size()) ;
	}
	
}
