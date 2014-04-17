package net.ion.craken.node.crud;

import java.util.List;
import java.util.concurrent.Future;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TranExceptionHandler;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.Debug;

public class TestWriteSession extends TestBaseCrud {

	public void testTran() throws Exception {
		assertEquals(false, session.exists("/test"));

		Future<Void> future = session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession tsession) {
				WriteNode node = tsession.pathBy("/test");
				node.property("name", "bleujin");

				return null;
			}
		});
		future.get();

		assertEquals(true, session.exists("/test"));
		ReadNode found = session.pathBy("/test");
		assertEquals("bleujin", found.property("name").value());
	}

	public void testPathByInTran() throws Exception {

		assertEquals(false, session.root().hasChild("/bleujin"));
		try {
			assertEquals(true, session.pathBy("/bleujin") != null);
		} catch (IllegalArgumentException expect) {
		}
		// assertEquals(true, session.root().child("/bleujin") != null) ;

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession tsession) {
				assertEquals(false, tsession.root().hasChild("/bleujin")); // create
				assertEquals(true, tsession.pathBy("/bleujin") != null);
				assertEquals(true, tsession.root().hasChild("/bleujin")); // created
				assertEquals(true, tsession.root().child("/bleujin") != null);

				return null;
			}
		}).get();

	}

	public void testContinueUnit() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 10; i++) {
					wsession.pathBy("/bleujin/" + i).property("name", "bleujin").property("index", i);
					if ((i % 2) == 0) {
						wsession.continueUnit();
					}
				}
				return null;
			}
		});

		session.ghostBy("/bleujin").children().debugPrint();
		Thread.sleep(1000);
		session.ghostBy("/bleujin").children().debugPrint();

	}

	public void testIgnoreIndex() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/index/0").property("index", 0).property("name", "bleujin");
				return null;
			}
		}).get();

		assertEquals(1, session.pathBy("/index").childQuery("name:bleujin").find().toList().size());

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/index/1").property("index", 1).property("name", "bleujin");
				return null;
			}
		}).get();

		assertEquals(2, session.pathBy("/index").childQuery("name:bleujin").find().toList().size());

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().ignore("name");
				wsession.pathBy("/index/2").property("index", 2).property("name", "hero");
				return null;
			}
		}).get();

		assertEquals(2, session.pathBy("/index").childQuery("name:bleujin").find().toList().size());

		assertEquals("hero", session.pathBy("/index/2").property("name").stringValue());

	}

	public void testQuery() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 10));
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				List<Fqn> list = wsession.queryRequest("").find().toFqns();
				int i = 0;
				for (Fqn fqn : list) {
					wsession.pathBy(fqn).property("new", 3);
				}
				return null;
			}
		});

		assertEquals(3, session.pathBy("/bleujin/1").property("new").intValue(0));
	}

	public void testExceptionHandler() throws Exception {
		final boolean fail = true;
		session.tranSync(new TransactionJob<Integer>() {
			@Override
			public Integer handle(WriteSession wsession) {
				if (fail)
					throw new IllegalArgumentException("fail");
				return 1;
			}
		}, new TranExceptionHandler() {
			@Override
			public void handle(WriteSession tsession, TransactionJob tjob, Throwable ex) {

			}
		});
	}

	public void testOnExecption() throws Exception {
		session.tran(new TransactionJob<Integer>() {
			@Override
			public Integer handle(WriteSession wsession) {
				throw new IllegalArgumentException("fail test");
			}
		}, new TranExceptionHandler() {
			@Override
			public void handle(WriteSession tsession, TransactionJob tjob, Throwable ex) {
				
			}
		});
	}

	
	public void testMakeSequence() throws Exception {
		TransactionJob<Long> nextVal = new TransactionJob<Long>() {
			@Override
			public Long handle(WriteSession wsession) throws Exception {
				long current = wsession.pathBy("/article").increase("sequence").asLong(0) ;
				return current;
			}
		};
		
		assertEquals(1, session.tranSync(nextVal).longValue());
		assertEquals(2, session.tranSync(nextVal).longValue());
		assertEquals(3, session.tranSync(nextVal).longValue());
		assertEquals(4, session.tranSync(nextVal).longValue());
		assertEquals(5, session.tranSync(nextVal).longValue());
	}
	
	public void testBatchInBatch() throws Exception {
		TransactionJob<Void> tran = new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").increase("sequence") ;
				session.tran(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/bleujin").property("name", "bleujin").increase("sequence") ;
						return null;
					}
				}) ;
				return null;
			}
		};

		session.tranSync(tran) ;
		Debug.line(session.pathBy("/bleujin").transformer(Functions.toJson())) ; 
	}
	
	
}
