package net.ion.craken.node.problem.distribute;

import java.io.File;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.RandomUtil;

public class TestNodeDistribute extends TestCase {

	private ReadSession session;
	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		new File("./resource/local").delete() ;
		
		this.r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherCacheStoreConfig.createDefault()) ;
		this.session = r.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	
	public void testRunThread() throws Exception {

		ExecutorService es = Executors.newCachedThreadPool();

		while (true) {
			es.submit(new ReadJobEntry(session));
			es.submit(new WriteJobEntry(session));
			Thread.sleep(1000) ;
		}

	}
}



class ReadJobEntry implements Callable<Void> {
	
	private ReadSession session ;
	public ReadJobEntry(ReadSession session){
		this.session = session ;
	}
	
	@Override
	public Void call() throws Exception {
		Debug.line(session.queryRequest("index:[1 TO 50]").offset(20).find().totalCount()) ;
		return null;
	}
}

class WriteJobEntry implements Callable<Void> {

	private ReadSession session ;
	public WriteJobEntry(ReadSession session){
		this.session = session ;
	}

	@Override
	public Void call() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				int idx = RandomUtil.nextInt(10);
				wsession.pathBy("/bleujin/" + new ObjectId().toString()).property("com", "bleujin").property("index", idx);
				return null;
			}
		});
		return null;
	}
}

