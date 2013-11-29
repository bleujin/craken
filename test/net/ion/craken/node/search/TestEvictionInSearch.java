package net.ion.craken.node.search;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;

public class TestEvictionInSearch extends TestCase{

	
	public void testEviction() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().maxNodeEntry(10).resetDir()) ;
		r.start() ;

		
		ReadSession session = r.login("test");
		
		session.tranSync(TransactionJobs.dummyEmp(20)) ;
		
		
		session.pathBy("/emp").children().debugPrint() ;
		
		
		r.shutdown() ;
		
	}
}
