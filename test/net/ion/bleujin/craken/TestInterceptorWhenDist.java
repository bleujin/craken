package net.ion.bleujin.craken;

import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.store.CrakenWorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.FileUtil;

import org.infinispan.configuration.cache.CacheMode;

public class TestInterceptorWhenDist extends TestCase {
	
	
	public void testRun() throws Exception {
		Craken craken = Craken.create() ;
		craken.createWorkspace("test", CrakenWorkspaceConfigBuilder.sifsDir("./resource/store/index", "./resource/store/data").distMode(CacheMode.REPL_SYNC)) ;
		
		craken.start() ;
		ReadSession session = craken.login("test") ;
		
		session.tran(TransactionJobs.HelloBleujin) ;
//		new InfinityThread().startNJoin(); 
		session.root().walkChildren().debugPrint();
		craken.stop(); 
	}
	
	public void testRun2() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/store/index2"));
		FileUtil.deleteDirectory(new File("./resource/store/data2"));
		Craken craken = Craken.create() ;
		craken.createWorkspace("test", CrakenWorkspaceConfigBuilder.sifsDir("./resource/store/index2", "./resource/store/data2").distMode(CacheMode.REPL_SYNC)) ;

		craken.start() ;
		ReadSession session = craken.login("test") ;
		
		session.root().walkChildren().debugPrint();
		
//		new InfinityThread().startNJoin();
		while(true){
			session.root().walkChildren().debugPrint();
			Thread.sleep(3000);
		}
		
//		craken.stop(); 
	}

	
	public void testConfirm() throws Exception {
		Craken craken = Craken.create() ;
		craken.createWorkspace("test", CrakenWorkspaceConfigBuilder.sifsDir("./resource/store/index2", "./resource/store/data2").distMode(CacheMode.REPL_SYNC)) ;

		craken.start() ;
		ReadSession session = craken.login("test") ;
		
		session.root().childQuery("", true).find().debugPrint();
		
		
		craken.stop();
	}
}
