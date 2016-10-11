package net.ion.bleujin;

import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.RandomUtil;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

public class TestConcurrent extends TestCase {

	private Craken icraken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.icraken = Craken.inmemoryCreateWithTest() ;
	}

	@Override
	protected void tearDown() throws Exception {
		icraken.stop();
		super.tearDown();
	}

	public void testCDDConcurrent() throws Exception {

		ExecutorService es = Executors.newCachedThreadPool(); // Executors.newSingleThreadExecutor() ; // 
		ReadSession rsession = icraken.login("test");
		
		Cache<String, FakeTask> tasks = CacheBuilder.newBuilder().maximumSize(100).build();
//		rsession.workspace().cddm().add(new CDDHandler() {
//			@Override
//			public String pathPattern() {
//				return "/thoth/{taskid}";
//			}
//
//			@Override
//			public TransactionJob<Void> modified(Map<String, String> rmap, CDDModifiedEvent mevent) {
//				final String taskId = rmap.get("taskid");
//				if (!"cancel".equals(mevent.property("action").asString()))
//					return null;
//				try {
//					Thread.sleep(700 + RandomUtil.nextInt(100));
//				} catch (InterruptedException e) {
//				}
//				return null;
//			}
//
//			@Override
//			public TransactionJob<Void> deleted(Map<String, String> map, CDDRemovedEvent cddremovedevent) {
//				return null;
//			}
//		});

		final CountDownLatch cd =  new CountDownLatch(30) ;
		for (int i = 0; i < 30; i++) {
			final String taskId = i+ "" ; // new ObjectId().toString();
			final FakeTask task = new FakeTask(taskId, this);
			tasks.put(taskId, task);

			rsession.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/thoth/" + taskId).property("name", "bleujin").property("startdate", new Date().getTime());
					return null;
				}
			});
			
			es.submit(new Callable<Void>() {
				public Void call() throws Exception {
					task.run();
					cd.countDown(); 
					return null;
				}
			});
		}
		
		cd.await(20, TimeUnit.SECONDS) ;
		Debug.line();
		icraken.login("test").ghostBy("/thoth").children().debugPrint();

	}


	public void endTask(final String taskId) {
		try {
			final ReadSession rsession = icraken.login("test");
//			rsession.tranSync(new TransactionJob<Void>() {
//				@Override
//				public Void handle(WriteSession wsession) throws Exception {
//					wsession.pathBy("/thoth/" + taskId).property("action", "cancel");
//					return null;
//				}
//			});
			rsession.tranSync(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					boolean removed = wsession.pathBy("/thoth").removeChild(taskId);
					return null;
				}
			});
			
//			rsession.tran(new TransactionJob<Void>() {
//				@Override
//				public Void handle(WriteSession wsession) throws Exception {
//					boolean removed = wsession.pathBy("/thoth").removeChild(taskId);
//					return null;
//				}
//			});
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}

class FakeTask implements Runnable {

	private TestConcurrent parent;
	private String taskId;

	public FakeTask(String taskId, TestConcurrent parent) {
		this.taskId = taskId;
		this.parent = parent;
	}

	@Override
	public void run() {
		for (int i = 0; i < RandomUtil.nextInt(10); i++) {
			if (shutdown)
				break;
			try {
				Thread.sleep(100 + RandomUtil.nextInt(10));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		parent.endTask(taskId);
	}

	private boolean shutdown;

	public boolean isShutdown() {
		return shutdown;
	}

	public void shutdown() {
		shutdown = true;
	}

}