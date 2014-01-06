package net.ion.bleujin.infinispan;

import java.io.File;
import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.RandomUtil;

import org.infinispan.remoting.transport.jgroups.SuspectException;

public class TestCentralCacheStore extends TestCase {


	public void testRepository() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		FileUtil.deleteDirectory(new File("./resource/ff5")) ;
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/ff5"));

		ReadSession session = r.login("test");
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 5; i++) {
					wsession.pathBy("/hero/" + i).property("idx", i);
				}
				return null;
			}
		});
		
		session.pathBy("/hero").children().debugPrint() ;

		r.shutdown();
	}
	
	public void testRead() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/ff5"));

		ReadSession session = r.login("test");
		session.pathBy("/bleujin").children().debugPrint();

		r.shutdown();

	}

	public void testRunning() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/ff5"));

		ReadSession session = r.login("test");
		while (true) {
			List<ReadNode> nodes = session.pathBy("/bleujin").children().toList();
			if (nodes.size() > 0) {
				Debug.line(nodes.size());
				try {
					session.tranSync(new TransactionJob<Void>() {
						@Override
						public Void handle(WriteSession wsession) throws Exception {
							final int nextInt = RandomUtil.nextInt(1000);
							wsession.pathBy("/bleujin/" + nextInt).property("name", "bleujin").property("index", nextInt) ;
							return null;
						}
					}) ;
				} catch(SuspectException expect) {
					Debug.line(expect) ;
				} catch(Throwable expect) {
					expect.printStackTrace();
				} ;
				Thread.sleep(7000);
			}
		}

	}

	
	
	public void testRemoveChild() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create());

		final ReadSession session = r.login("test");
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/a/b/c").property("name", "line") ;
				wsession.pathBy("/a/b").property("name", "c") ;
				return null;
			}
		}).get() ;
		
		assertEquals(true, session.exists("/a/b/c")) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				assertEquals(false, wsession.pathBy("/a/b/").removeChild("c/d/e")) ; // illegal
				assertEquals(true, wsession.pathBy("/a").removeChild("b")) ;
				
				return null;
				
			}
		}).get() ;
		
		assertEquals(false, session.exists("/a/b")) ; // exist child
//		assertEquals("c", session.pathBy("/a/b/c").property("name").value()) ;
	}

	
	
	public void testReplactModify() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create());

		final ReadSession session = r.login("test");
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.pathBy("/bleujin").property("name", "hero").property("age", 21) ;
				return null ;
			}}).get() ;
		
		assertEquals("hero", session.pathBy("/bleujin").property("name").value()) ;
	}
}
