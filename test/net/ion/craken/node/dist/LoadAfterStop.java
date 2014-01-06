package net.ion.craken.node.dist;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;

public class LoadAfterStop extends TestCase {

	final String location = "./resource/test";

	public void testView() throws Exception {
		// run xtestWriter() ;
		// run xtestReader() ;
	}
	
	public void xtestReader() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location(location));
		ReadSession session = r.login("test");

		if (session.exists("/bleujin")) {
			Debug.line(session.pathBy("/bleujin").children().toList().size());
		}
	}

	public void xtestConfirm() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location(location));
		ReadSession session = r.login("test");

		session.workspace().central().newSearcher().createRequest("").find().debugPrint();

		r.shutdown();
	}

	public void xtestWriter() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location(location));
		ReadSession session = r.login("test");

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int num = 0; num < 10; num++) {
					wsession.pathBy("/bleujin/" + num).property("num", num);
				}
				return null;
			}
		});

		r.shutdown();
	}
}
