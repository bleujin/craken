package net.ion.craken.node.problem.speed;

import java.util.Iterator;

import com.google.common.base.Function;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherCacheStoreConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ReadNodeImpl;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.problem.store.SampleWriteJob;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;

public class TestInsertSpeed extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherCacheStoreConfig.create().location("./resource/local").maxEntries(10).chunkSize(1024 * 1024 * 10));
		r.start();
		this.session = r.testLogin("test");
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	// 30sec per 20k(create, about 700 over per sec 6.21, 44M)
	// 18sec per 20k(update, about 1k lower per sec 6.21 70M)
	public void testCreateWhenEmpty() throws Exception {
		
		int loopCount = 100 ;
//		session.tranSync(new TransactionJob<Void>() {
//			@Override
//			public Void handle(WriteSession wsession) throws Exception {
//				wsession.root().removeChildren() ;
//				return null;
//			}
//		}) ;
		
		long start = System.currentTimeMillis();
		session.tranSync(new SampleWriteJob(100));

		Integer result = session.root().children().transform(new Function<Iterator<ReadNode>, Integer>() {
			private int count = 0 ;
			public Integer apply(Iterator<ReadNode> nodes) {
				while(nodes.hasNext()){
					count++ ;
					nodes.next() ;
				}
				return count;
			}
		});
		
		assertEquals(loopCount, result.intValue()) ;
//		
//		Debug.line(System.currentTimeMillis() - start);
	}
	
	// 18, 14, 14, 15, 14  -> 95M
	public void xtestLoop() throws Exception {
		for (int i = 0; i < 5; i++) {
			testCreateWhenEmpty() ;
		}
	}

	public void testFind() throws Exception {
		long start = System.currentTimeMillis();
		for (int i : ListUtil.rangeNum(20)) {
			Debug.line(session.pathBy("/" + RandomUtil.nextInt(200)).toMap());
		}
		Debug.line(System.currentTimeMillis() - start);

	}

}
