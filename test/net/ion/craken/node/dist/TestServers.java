package net.ion.craken.node.dist;

import java.io.File;

import junit.framework.TestCase;

import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.InfinityThread;

public class TestServers extends TestCase {
	
	
	public void testRepoId() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/c1")) ;
		final RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c1")) ;
		
		r.start() ;

		assertEquals("emanon", r.memberId());
		ReadSession session = r.login("test");
		assertEquals("emanon", session.pathBy("/__servers/" + r.addressId()).property("repoid").stringValue()) ;

		r.shutdown() ;
	}
	
	public void xtestServer1() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/c1")) ;
		final RepositoryImpl r = RepositoryImpl.create("bleujin");
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c1")) ;
		
		r.start() ;
		assertEquals("bleujin", r.memberId());
		ReadSession session = r.login("test");
		assertEquals("bleujin", session.pathBy("/__servers/" + r.addressId()).property("repoid").stringValue()) ;

		new InfinityThread().startNJoin() ;
	}
	
	public void xtestServer2() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/c2")) ;
		final String newRepoId = "hero";
		
		final RepositoryImpl r = RepositoryImpl.create(newRepoId);
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c2")) ;
		
		r.start() ;
		assertEquals(newRepoId, r.memberId());
		ReadSession session = r.login("test");
		assertEquals(newRepoId, session.pathBy("/__servers/" + r.addressId()).property("repoid").stringValue()) ;
		
		
		r.shutdown() ;
		
	}
	
	
	
	
}
