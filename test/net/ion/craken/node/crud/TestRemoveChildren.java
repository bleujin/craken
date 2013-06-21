package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.problem.store.SampleWriteJob;
import net.ion.framework.util.Debug;

public class TestRemoveChildren extends  TestCase {

	
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
	
	public void testLoad() throws Exception {
//		session.tranSync(new SampleWriteJob(20));
		ReadChildren children = session.root().children();
		while(children.hasNext()){
			Debug.line(children.next().fqn().toString()) ;
		}
	}
	
	public void testRemoveAfter() throws Exception {
		session.tranSync(new SampleWriteJob(20));
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root().removeChildren() ;
				return null;
			}
		}) ;
		session.tranSync(new SampleWriteJob(20));
		
		session.pathBy("/10").fqn() ;
		
		session.root().children().debugPrint() ;
	}
}
