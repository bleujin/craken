package net.ion.craken.loaders.lucene;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;

public class TestJava extends TestCase {

	public void testBlockingQueue() throws Exception {
		ExecutorService ex = Executors.newCachedThreadPool();

		final ArrayBlockingQueue<String> queue = new ArrayBlockingQueue<String>(200000);
		ex.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				for (int i = 0; i < 100000; i++) {
					queue.put("" + i);
				}
				return null;
			}
		});
		
		
		ex.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				long start = System.currentTimeMillis() ;
				for (int i = 0; i < 100000; i++) {
					queue.take();
				}
				Debug.line(System.currentTimeMillis() - start) ;
				return null;
			}
		}) ;

		ex.shutdown() ;

		
		new InfinityThread().startNJoin() ;
	}

}
