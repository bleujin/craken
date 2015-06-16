package net.ion.craken.problem;

import java.io.File;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;

import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockFactory;
import org.infinispan.configuration.cache.CacheMode;

import junit.framework.TestCase;

public class TestLockFactory extends TestCase {
	
	public void testLockFactory() throws Exception {
		final Craken craken = Craken.create();

		craken.createWorkspace("ics", WorkspaceConfigBuilder.indexDir("").distMode(CacheMode.DIST_SYNC));
		
		ReadSession session = craken.login("ics") ;
		
		Directory dir = session.workspace().central().dir() ;
		
		LockFactory lf = dir.getLockFactory();
		Debug.line(lf) ;
		
		
	}
	
	public void testFirst() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/first"));
		final Craken r = Craken.create();

		r.createWorkspace("ics", WorkspaceConfigBuilder.indexDir("./resource/temp/first").distMode(CacheMode.DIST_SYNC));
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});

		ReadSession session = r.login("ics");

		int count = 0;
		while (true) {
			Thread.sleep(1000);
			final int index = count++;
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/index", index).property("index", index);
					return null;
				}
			});
			if (index == 1) Debug.line("started");
		}
	}


	public void testSecond() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/second"));
		
		final Craken r = Craken.create();
		r.createWorkspace("ics", WorkspaceConfigBuilder.indexDir("./resource/temp/second").distMode(CacheMode.DIST_SYNC));

		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});
		


		ReadSession session = r.login("ics");
		int count = 1000;
		while(true){
			Thread.sleep(1000);
			final int index = count++;
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/index", index).property("index", index);
					return null;
				}
			});
			Debug.line(session.ghostBy("/index").children().count()) ;
		}
	}
	
}
