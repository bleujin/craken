package net.ion.craken.node.problem.distribute;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.RandomUtil;

public class TestNodeDistribute extends TestBaseCrud {

	public void testRunThread() throws Exception {
		ExecutorService es = Executors.newCachedThreadPool();

		final Callable<Void> pd = new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				session.tran(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						int idx = RandomUtil.nextInt(100);
						wsession.pathBy("/main/" + idx).property("index", idx);
						return null;
					}
				});
				return null;
			}
		};

		final Callable<Void> cs = new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				Debug.line() ;
				session.queryRequest("index:[1 TO 50]").find().debugPrint();
				return null;
			}
		};

		while (true) {
			es.submit(cs);
			es.submit(pd);
			Thread.sleep(1000) ;
		}

	}

}
