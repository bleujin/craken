package net.ion.craken.node.problem;

import java.util.concurrent.CountDownLatch;

import net.ion.craken.node.DefaultTranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestTransaction  extends TestBaseCrud {

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.root().child("hero").property("name", "hero").property("age", 30L) ;
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


	public void testDefault() throws Exception {
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").stringValue()) ;
		assertEquals("hero", session.pathBy("/hero").property("name").stringValue()) ;
	}
	
	
	public void testFailSession() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1) ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").clear() ;
				throw new IllegalArgumentException("fail") ;
			}
		}, new DefaultTranExceptionHandler() {
			@Override
			public void handle(WriteSession tsession, Throwable ex) {
				latch.countDown();
			}
		}).get() ;

		latch.await() ;
		assertEquals(2, session.pathBy("/bleujin").normalKeys().size()) ; // not cleard
	}
	
	public void testFailWriteNode() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/jin").property("name", "beujin");
				throw new IllegalArgumentException("fail") ;
			}
		}, new DefaultTranExceptionHandler() {
			@Override
			public void handle(WriteSession tsession, Throwable ex) {
				// 
			}
		}).get() ;

		assertEquals(false, session.exists("/jin")) ;
	}
	
	public void testAllRollback() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/hero").property("name", "mod");
				wsession.pathBy("/jin").property("name", "beujin");
				throw new IllegalArgumentException("fail") ;
			}
		}, new DefaultTranExceptionHandler() {
			@Override
			public void handle(WriteSession tsession, Throwable ex) {
				// 
			}
		}).get() ;
		assertEquals(false, session.exists("/jin")) ;
		assertEquals("hero", session.pathBy("/hero").property("name").stringValue()) ;
	}
	
}
