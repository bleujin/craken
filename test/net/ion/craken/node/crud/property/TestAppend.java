package net.ion.craken.node.crud.property;

import java.util.Set;
import java.util.concurrent.CountDownLatch;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.SetUtil;

public class TestAppend extends TestBaseCrud {

	public void testAppend() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("/bleujin").append("no", 1, 2, 3);
				return null;
			}
		}).get();

		ReadNode bleujin = session.pathBy("/bleujin");
		assertEquals(1, bleujin.property("no").value());

		Set set = bleujin.property("no").asSet();

		assertEquals(3, set.size());

		assertEquals(true, set.contains(1));
		assertEquals(true, set.contains(2));
		assertEquals(true, set.contains(3));
	}

	public void testAppendAfterProperty() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("/bleujin").property("no", 1).append("no", 2, 3);
				return null;
			}
		}).get();

		ReadNode bleujin = session.pathBy("/bleujin");
		assertEquals(1, bleujin.property("no").value());

		Set set = bleujin.property("no").asSet();

		assertEquals(3, set.size());

		assertEquals(true, set.contains(1));
		assertEquals(true, set.contains(2));
		assertEquals(true, set.contains(3));
	}

	public void testPropertyAfterAppend() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("/bleujin").append("no", 2, 3).property("no", 1);
				return null;
			}
		}).get();

		ReadNode bleujin = session.pathBy("/bleujin");
		assertEquals(1, bleujin.property("no").value());

		Set set = bleujin.property("no").asSet();

		assertEquals(1, set.size());
	}
	
	public void testContains() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("/bleujin").append("receiver", "jin", "hero").property("no", 1);
				return null;
			}
		}).get();
		
		

		ReadNode bleujin = session.pathBy("/bleujin");
		
		assertTrue(bleujin.property("receiver").asSet().contains("jin")) ;
		assertTrue(bleujin.property("receiver").asSet().contains("hero")) ;
		assertFalse(bleujin.property("receiver").asSet().contains("bleujin")) ;
	}

	public void testSetArray() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("/bleujin").property("no", new int[] { 1, 2, 3 }).property("str", new String[] { "bleu", "jin" });
				return null;
			}
		}).get();

		assertEquals(1, session.pathBy("/bleujin").property("no").value());
		assertEquals(3, session.pathBy("/bleujin").property("no").asSet().size());

		assertEquals("bleu", session.pathBy("/bleujin").property("str").value());
		assertEquals(2, session.pathBy("/bleujin").property("str").asSet().size());
	}

	public void testSetBlankArray() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("/bleujin").property("str", new String[] {});
				return null;
			}
		}).get();

		assertEquals(true, session.pathBy("/bleujin").hasProperty("str")) ;
		
		assertEquals("", session.pathBy("/bleujin").property("str").asString());
		assertEquals(0, session.pathBy("/bleujin").property("str").asSet().size());
	}

	
	
	public void testDisAllowDiffType() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1) ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().child("/bleujin").append("no", "1", 2);
				return null;
			}
		}, new TranExceptionHandler() {
			@Override
			public void handle(WriteSession tsession, TransactionJob tjob, Throwable ex) {
				latch.countDown() ;
			}
		});
		
		latch.await() ;
	}
	
	public void testAppendMethod() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/hero").append("name", "bleujin").append("age", 20, 30) ;
				wsession.pathBy("/bleujin").append("name", new Object[]{"bleujin"}).append("age", new Object[]{20, 30}) ;
				return null;
			}
		}) ;
		
		
		print(session.pathBy("/bleujin").property("name"));
		print(session.pathBy("/bleujin").property("age"));
		print(session.pathBy("/hero").property("name"));
		print(session.pathBy("/hero").property("age"));

	}
	
	public void testBlankAfterUnsetAll() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/hero").append("name", "bleujin").append("age", 20, 30) ;
				return null;
			}
		}) ;

		assertEquals(true, session.pathBy("/hero").hasProperty("age")) ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/hero").unset("age", 20) ;
				wsession.pathBy("/hero").unset("age", 30) ;
				return null;
			}
		}) ;
		
		session.pathBy("/hero").debugPrint();
		Debug.line(false, session.pathBy("/hero").property("age").asSet().size()) ;
	}
	
	
	public void testArrayAtProperty() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/hero").property("name", new String[]{"jin", "hero"}) ;
				return null;
			}
		}) ;
		Debug.line(session.pathBy("/hero").property("name").asSet()) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/hero").property("name", new String[]{"bleu"}) ;
				return null;
			}
		}) ;
		Debug.line(session.pathBy("/hero").property("name").value(), session.pathBy("/hero").property("name").asSet()) ;
	}
	
	
	
	private void print(PropertyValue prop){
		Debug.debug(prop.stringValue()) ;
		Debug.debug(prop.asSet(), prop.asSet().size()) ;
	}

}
