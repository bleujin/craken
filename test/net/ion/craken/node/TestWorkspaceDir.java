package net.ion.craken.node;

import java.io.File;

import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import junit.framework.TestCase;

public class TestWorkspaceDir extends TestCase{

	
	public void testIndexDir() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/index"));
		Craken craken = Craken.local() ;
		
		
		craken.createWorkspace("index", WorkspaceConfigBuilder.indexDir("./resource/temp/index")) ;
		craken.createWorkspace("grid", WorkspaceConfigBuilder.gridDir("./resource/temp/grid")) ;
		craken.createWorkspace("sifs", WorkspaceConfigBuilder.sifsDir("./resource/temp/sifs")) ;
		craken.createWorkspace("old", WorkspaceConfigBuilder.oldDir("./resource/temp/old")) ;
		
		
		String[] wnames = new String[]{"index", "grid", "sifs", "old"} ;
		for (String wname : wnames) {
			craken.login(wname).tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					for(int idx = 0 ; idx++ < 4; ){
						wsession.pathBy("/index", idx).property("index", idx) ;
					}
					return null;
				}
			}) ;
		}
		
		craken.stop(); 

		
		craken = Craken.local() ;
		
		craken.createWorkspace("index", WorkspaceConfigBuilder.indexDir("./resource/temp/index")) ;
		craken.createWorkspace("grid", WorkspaceConfigBuilder.gridDir("./resource/temp/grid")) ;
		craken.createWorkspace("sifs", WorkspaceConfigBuilder.sifsDir("./resource/temp/sifs")) ;
		craken.createWorkspace("old", WorkspaceConfigBuilder.oldDir("./resource/temp/old")) ;

		
		for (String wname : wnames) {
			Debug.line(wname);
			assertEquals(5, craken.login(wname).root().walkChildren().count()) ;
			assertEquals(5, craken.login(wname).root().childQuery("", true).find().totalCount()) ;
//			craken.login(wname).root().walkChildren().debugPrint(); 
		}
		craken.stop();
		
	}
}
