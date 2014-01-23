package net.ion.craken.node.convert.rows;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import com.sun.org.apache.xalan.internal.xsltc.compiler.sym;

import junit.framework.TestCase;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;

public class TestList extends TestCase {

	public void testSequence() throws Exception {
		final Seq seq = new Seq();

		ExecutorService ex = Executors.newFixedThreadPool(5);

		List<Future> futures = ListUtil.newList();

		for (int i = 0; i < 5; i++) {
			Future<Void> future = ex.submit(new Callable<Void>() {
				@Override
				public Void call() throws Exception {
					for (int i = 0; i < 100000000; i++) {
						seq.increase();
					}
					return null;
				}
			});
			futures.add(future);
		}

		for (Future f : futures) {
			f.get();
		}

		assertEquals(5 * 100000000 + 1, seq.increase());

	}

	public void testConcurrent() throws Exception {

		final ArrayList<String> list = new ArrayList<String>();
		// final List<String> list = new CopyOnWriteArrayList<String>() ;

		ExecutorService es = Executors.newFixedThreadPool(5);

		final int maxLoop = 30000;
		Future<Void> future1 = es.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				for (int i = 0; i < maxLoop; i++) {
					list.add(i + "");
				}
				return null;
			}
		});

		Future<Void> future3 = es.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				for (int i = 0; i < maxLoop; i++) {
						// list.remove(RandomUtil.nextInt(Math.max(list.size(), 1)) ;
				}
				return null;
			}
		});

		Future<Void> future2 = es.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				for (int i = 0; i < 50000; i++) {
					for (String str : list) {

					}
				}
				return null;
			}
		});

		future1.get();
		future2.get();
		future3.get();
	}

}

class Seq {
	// private AtomicInteger no = new AtomicInteger(0) ;
	// public int increase(){
	// return no.incrementAndGet() ;
	// }

	private int no = 0;

	public int increase() {
		return no++;
	}

}
