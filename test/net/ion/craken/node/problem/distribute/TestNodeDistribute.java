package net.ion.craken.node.problem.distribute;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.RandomUtil;

public class TestNodeDistribute extends TestCase {

	private RepositoryImpl r;
	private ExecutorService workerPool;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
//		FileUtil.deleteDirectory(new File("./resource/local")) ;
		
		this.r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.createDefault()) ;
		r.start() ;
		this.workerPool = Executors.newCachedThreadPool();
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		workerPool.shutdown() ;
		super.tearDown();
	}
	
	
	public void testReadNWrite() throws Exception {
		while (true) {
			workerPool.submit(new ReadJobEntry(r));
			workerPool.submit(new WriteJobEntry(r));
			Thread.sleep(500) ;
		}
	}
	
	public void testRead() throws Exception {
		while (true) {
			workerPool.submit(new ReadJobEntry(r));
			Thread.sleep(500) ;
		}
	}
	
	
}



class ReadJobEntry implements Callable<Void> {
	
	private ReadSession session ;
	public ReadJobEntry(Repository r) throws IOException{
		this.session = r.login("test") ;
	}
	
	@Override
	public Void call() throws Exception {
		Debug.line(session.queryRequest("index:[1 TO 50]").offset(20).find().totalCount()) ;
		return null;
	}
}

class WriteJobEntry implements Callable<Void> {

	private ReadSession session ;
	public WriteJobEntry(Repository r) throws IOException{
		this.session = r.login("test") ;
	}

	@Override
	public Void call() throws Exception {
		return session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				int idx = RandomUtil.nextInt(10);
				wsession.pathBy("/bleujin/" + new ObjectId().toString()).property("com", "bleujin").property("index", idx);
				return null;
			}
		});
	}
}

