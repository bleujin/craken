package net.ion.craken.node.search;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;

public class TestIndex extends TestBaseSearch {

	
	public void testConcurrency() throws Exception {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					r.login("test").queryRequest("bleujin").find().debugPrint();
					r.login("test").tran(new TransactionJob<Void>() {
						@Override
						public Void handle(WriteSession wsession) {
							wsession.root().child("/" + RandomUtil.nextInt(3)).property("name", new String[] { "jin", "bleujin", "hero" }[RandomUtil.nextInt(3)]);
							return null;
						}
					}).get();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		ExecutorService exec = Executors.newFixedThreadPool(5);
		for (int i : ListUtil.rangeNum(50)) {
			exec.submit(task);
		}
//		exec.shutdown() ;
		exec.awaitTermination(5, TimeUnit.SECONDS);
		exec.shutdownNow() ;
	}

	public void testIndex() throws Exception {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					session.tranSync(new TransactionJob<Void>() {
						@Override
						public Void handle(WriteSession wsession) {
							wsession.root().child("/bleujin").property("name", "bleujin").property("age", RandomUtil.nextInt(50));
							wsession.root().child("/hero").property("name", "hero").property("age", 25);
							wsession.root().child("/jin").property("name", "jin").property("age", 30);
							return null;
						}
					});
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		ExecutorService indexexec = Executors.newFixedThreadPool(3);
		for (int i : ListUtil.rangeNum(100)) {
			indexexec.submit(task);
		}
		Thread.sleep(500) ;

		for (int i = 0; i < 100; i++) {
			ReadSession other = r.login("test");
			ChildQueryResponse response = other.queryRequest("bleujin").find();
			Debug.line(i, response.size(), response.first());
		}

		indexexec.awaitTermination(5, TimeUnit.SECONDS);
		assertEquals("test", session.workspace().wsName());

	}

}
