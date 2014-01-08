package net.ion.craken.node.dist;

import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.InfinityThread;

public class TestResync extends TestCase{

	
	public void testRunMain() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/c1")) ;
		final RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c1")) ;
		
		r.start() ;

		ReadSession session = r.login("test");
		
		session.tranSync(TransactionJobs.dummy("/bleujin", 3)) ;
		session.tranSync(TransactionJobs.dummy("/hero", 3)) ;
		session.tranSync(TransactionJobs.dummy("/jin", 3)) ;
		
		
		new InfinityThread().startNJoin() ;
	}
	
	
	public void testResync() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/c2")) ;
		final RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c2").maxNodeEntry(4)) ;
		
		r.start() ;
		
//		new InfinityThread().startNJoin() ;
		
		ReadSession session = r.login("test");
		session.pathBy("/bleujin").children().debugPrint() ;
		session.pathBy("/hero").children().debugPrint() ;
		session.pathBy("/jin").children().debugPrint() ;
		
		Thread.sleep(2000) ;
		r.shutdown() ;
	}
	
	
	
}
