package net.ion.bleujin.craken;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;

import org.infinispan.configuration.cache.CacheMode;

public class TestMemoryWorkspace2 extends TestCase{

	private Craken craken;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

		this.craken = Craken.create();
		
		craken.createWorkspace("test", WorkspaceConfigBuilder.memoryDir().distMode(CacheMode.REPL_SYNC)) ;
		this.session = craken.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.shutdown() ;
		super.tearDown();
	}
	
	public void testAsync() throws Exception {

		
		ExecutorService es = Executors.newFixedThreadPool(10);
		final long start = System.currentTimeMillis() ;
		
		Callable<Void> call = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				session.tran(new TransactionJob<Void>(){
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/emp/bleujin").property("name", "bleujin") ;
						wsession.pathBy("/emp/hero").property("name", "hero") ;
						System.out.print('.');
						return null;
					}
				}) ;
				return null;
			}
		};

		Callable<Void> cal2 = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				session.tran(new TransactionJob<Void>(){
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/emp/hero").property("name", "hero") ;
						wsession.pathBy("/emp/bleujin").property("name", "bleujin") ;
						System.out.print('.');
						return null;
					}
				}) ;
				return null;
			}
		};

		for (int i = 0; i < 1000; i++) {
			Future<Void> future = (RandomUtil.nextInt() % 2 == 0) ? es.submit(call) : es.submit(cal2) ;
		}
		
		
		
		
		new InfinityThread().startNJoin(); 
	}
	
}
