package net.ion.craken.node.problem.store;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.node.problem.TestConfig;
import net.ion.framework.util.InfinityThread;

public class TestDistDefault extends TestCase {
	
	
	
	public void testLoadInCache() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineConfig("test.node", TestConfig.createFastLocalCacheStore(5));

		ReadSession session = r.login("test");
		session.tranSync(TransactionJobs.dummy("/bleujin", 10));

		session.pathBy("/bleujin").children().debugPrint();

		new InfinityThread().startNJoin();
		r.shutdown();
	}
}
