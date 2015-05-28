package net.ion.bleujin.working;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.configuration.cache.CacheMode;

public class TestStoreRepl extends TestCase {
	private RepositoryImpl r;
	private ReadSession session;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
		r.createWorkspace("swpace", WorkspaceConfigBuilder.directory("./resource/store/search").distMode(CacheMode.DIST_SYNC)) ;
		
		this.session = r.login("swpace") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}

	public void testSave() throws Exception {
		long start = System.currentTimeMillis() ;
		session.tran(TransactionJobs.dummy("/bleujin", 200)) ;
		Debug.line(System.currentTimeMillis() - start);
	}

	public void testRun() throws Exception {
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				r.shutdown() ;
			}
		});
		new InfinityThread().startNJoin(); 
	}
	
	public void testConfirm() throws Exception {
		session.pathBy("/airkjh").children().offset(3).debugPrint();
	}
}
