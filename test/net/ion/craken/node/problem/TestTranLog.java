package net.ion.craken.node.problem;

import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.TestListener.DebugListener;
import net.ion.craken.node.problem.store.SampleWriteJob;

public class TestTranLog extends TestBaseCrud{

	
	public void testChildren() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals(1, session.root().children().toList().size()) ;
	}
	
	public void testRemoveChildren() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		assertEquals(1, session.root().children().toList().size()) ;
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root().removeChildren() ;
				return null;
			}
		}) ;
		assertEquals(0, session.root().children().toList().size()) ;
	}
	
	public void testListener() throws Exception {
		
	}
	
	public void testKey() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		assertEquals(1, session.pathBy("/bleujin").normalKeys().size()) ;
	}
	
	public void testRoot() throws Exception {
		tearDown() ;
		this.r = RepositoryImpl.create() ;
		this.config = CentralCacheStoreConfig.createDefault() ;
		r.defineWorkspaceForTest("test", config.maxNodeEntry(10)) ;
		
		r.start() ;
		this.session = r.login("test") ;

		assertEquals(true, session.exists("/")) ;
		session.tranSync(new SampleWriteJob(20));
		session.root().children().debugPrint() ;
	}

	
	public void testAddListener() throws Exception {
		final DebugListener listener = new DebugListener();
		session.workspace().addListener(listener) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("bleujin").property("name", "bleujin");
				return null ;
			}
		}).get() ;
		
		assertEquals(1, listener.getCount()) ;
	}
	
	
}
