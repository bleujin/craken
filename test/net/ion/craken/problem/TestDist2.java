package net.ion.craken.problem;

import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

import org.infinispan.configuration.cache.CacheMode;

public class TestDist2 extends TestCase {

	public void testFirst() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/first"));
		final Craken r = Craken.create();

		r.createWorkspace("ics", WorkspaceConfigBuilder.indexDir("").distMode(CacheMode.DIST_SYNC));
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});

		ReadSession session = r.login("ics");

		
		new InfinityThread().startNJoin(); 
		Central central = session.workspace().central() ;
		
		int count = 0;
		while (true) {
			Thread.sleep(1000);
			final int index = count++;
			central.newIndexer().index(new IndexJob<Void>() {
				@Override
				public Void handle(IndexSession isession) throws Exception {
					isession.newDocument().keyword("name", "bleujin").update() ;
					return null;
				}
			}) ;
			if (index == 1) Debug.line("started");
		}
	}

	public void testSecond() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/second"));
		
		final Craken r = Craken.create();
		r.createWorkspace("ics", WorkspaceConfigBuilder.indexDir("").distMode(CacheMode.DIST_SYNC));

		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});


		ReadSession session = r.login("ics");
		Central central = session.workspace().central() ;
		while(true){
			Thread.sleep(1000);
			Debug.line(central.newSearcher().createRequest("").find().totalCount()) ;
		}
		
		
	}

}
