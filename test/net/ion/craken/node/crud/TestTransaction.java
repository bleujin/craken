package net.ion.craken.node.crud;

import java.lang.ref.WeakReference;
import java.util.concurrent.CountDownLatch;

import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;

public class TestTransaction  extends TestBaseCrud {

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.root().addChild("hero").property("name", "hero").property("age", 30L) ;
				return null;
			}
		}).get() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().removeChildren() ;
				return null;
			}
		}).get() ;
		super.tearDown();
	}


	
	public void testFailSession() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1) ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").clear() ;
				throw new IllegalArgumentException("fail") ;
			}
		}, new TranExceptionHandler() {
			@Override
			public void handle(WriteSession tsession, Throwable ex) {
				latch.countDown();
			}
		}).get() ;

		latch.await() ;
		assertEquals(2, session.pathBy("/bleujin").keys().size()) ;
		
	}
	
}
