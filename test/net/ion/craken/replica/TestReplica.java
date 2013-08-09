package net.ion.craken.replica;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.loaders.lucene.DocEntry;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.InfinityThread;

public class TestReplica extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/c1"));
		r.defineWorkspace("test2", CentralCacheStoreConfig.create().location("./resource/c2"));
		r.start() ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	
	public void testStartEvent() throws Exception {
//		session.root().children().debugPrint() ;
		String s = "" ;
		session.tran(TransactionJobs.dummy("/bleujin", 5)) ;
		new InfinityThread().startNJoin() ;
	}
	
	public void testLastModified() throws Exception {
//		session.tranSync(TransactionJobs.dummy("/bleujin", 5)) ;

//		session.queryRequest("").gt(DocEntry.LASTMODIFIED, System.currentTimeMillis() - 10000).find().debugPrint() ;
		new InfinityThread().startNJoin() ;
	}
}
