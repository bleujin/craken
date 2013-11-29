package net.ion.craken.node.problem;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;

public class TestMaxEntry extends TestCase {
	
	protected RepositoryImpl r ;
	protected ReadSession session;
	protected CentralCacheStoreConfig config;

	@Override
	protected void setUp() throws Exception {
		this.r = RepositoryImpl.create() ;
		this.config = CentralCacheStoreConfig.createDefault().maxNodeEntry(1);
		r.defineWorkspace("test", config) ;
		
		r.start() ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	
	public void testCacheOne() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 20)) ;
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin/1").property("dummy", 3000.0) ;
				return null;
			}
		}) ;
		Debug.line(session.pathBy("/bleujin/1").property("dummy").intValue(0)) ;
	}
}
