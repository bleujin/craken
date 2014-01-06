package net.ion.craken.node.dist;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;

public class TestRestoreOnStart extends TestCase {

	public void testStart() throws Exception {
		// run xtestWriter
		// run xtestAfterReader
	}

	public void xtestWriter() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		
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

	public void xtestAfterReader() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest() ;
		
		ReadSession session = r.login("test");
		
		try {
			for (int i = 0; i < 150; i++) {
				Debug.line(session.workspace().logmeta().size()) ;
				Thread.sleep(1000);
			}
		} finally {
			r.shutdown();
		}
	}

	
	
}
