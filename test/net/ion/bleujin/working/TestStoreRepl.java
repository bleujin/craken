package net.ion.bleujin.working;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

import org.infinispan.configuration.cache.CacheMode;

import com.sun.corba.se.impl.activation.RepositoryImpl;

public class TestStoreRepl extends TestCase {

	private Craken r;
	private ReadSession session;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.create() ;
		r.createWorkspace("swpace", OldFileConfigBuilder.directory("./resource/store/search").distMode(CacheMode.DIST_SYNC)) ;
		
		this.session = r.login("swpace") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}

	public void testSave() throws Exception {
		long start = System.currentTimeMillis() ;
		session.tran(TransactionJobs.dummy("/airkjh", 200)) ;
		Debug.line(System.currentTimeMillis() - start);
	}
	
	public void testRemove() throws Exception {
		session.tran(new TransactionJob<Void>() {

			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/airkjh").removeSelf() ;
				return null;
			}
		}).get() ;
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
