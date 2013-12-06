package net.ion.craken.node.dist;

import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;

public class TestAtSingleCom extends TestCase {

	
	public void testStart() throws Exception {
		// run xtestReader
		// run xtestWriter
	}
	
	public void xtestReader() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/c1"));
//		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest() ;
		
		ReadSession session = r.login("test");

		try {
			for (int i = 0; i < 150; i++) {
				if (session.exists("/bleujin")) {
					session.pathBy("/bleujin").children().toList().size();
				}
				Thread.sleep(1000);
			}
		} finally {
			r.shutdown();
		}
	}

	public void xtestConfirm() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/c1"));
		ReadSession session = r.login("test");

		session.workspace().central().newSearcher().createRequest("").find().debugPrint();
		
		r.shutdown() ;
	}
	
	
	public void xtestWriter() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/c2")) ;
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/c2"));
		
//		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest() ;

		ReadSession session = r.login("test");
		try {
			for (int i = 0; i < 100; i++) {
				final int num = i;
				session.tranSync(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/bleujin/" + num).property("num", num);
						return null;
					}
				});
				Thread.sleep(1000);
			}
		} finally {
			r.shutdown();
		}
	}

}
