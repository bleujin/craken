package net.ion.craken.node.crud;

import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.loaders.lucene.OldCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.problem.store.SampleWriteJob;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ListUtil;

public class TestRemoveChildren extends  TestCase {

	
	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
		FileUtil.deleteDirectory(new File("./resource/local")) ;
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/local").maxNodeEntry(10));
		r.start();
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}


	
	
	public void testRemoveAfter() throws Exception {
		assertEquals(true, session.exists("/")) ;
		assertEquals(true, session.exists("/")) ;

//		session.tranSync(new SampleWriteJob(20));
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 20; i++) {
					wsession.createBy("/" + i).property("idx", i) ;
				}
				return null;
			}
		}) ;
		
	 	assertEquals(20, session.root().children().toList().size()) ;
	 	assertEquals(true, session.exists("/")) ;
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root().removeChildren() ;
				return null;
			}
		}) ;
		
	 	assertEquals(0, session.root().children().toList().size()) ;
	 	assertEquals(true, session.exists("/")) ;

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i : ListUtil.rangeNum(20)) {
					final WriteNode wnode = wsession.pathBy("/bleujin/" + i);
					wnode.property("key", "val") ;
				}
				return null;
			}
		}) ;

		assertEquals(true, session.exists("/")) ;
		
	 	assertEquals(20, session.pathBy("/bleujin").children().toList().size()) ;
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root().removeChildren() ;
				return null;
			}
		}) ;
	 	assertEquals(0, session.root().children().toList().size()) ;
	}
	
	
}
