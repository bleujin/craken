package net.ion.bleujin.infinispan;

import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

public class TestInfinispanDir extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/ff2")) ;
		this.session = r.login("test") ;
		r.start() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testWrite() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 15000; i++) {
					wsession.pathBy("/bleujin/" + i).property("index", i) ;
				}
				return null;
			}
		}) ;
	}
	
	public void testRunning() throws Exception {
		while(true){
			final List<ReadNode> children = session.ghostBy("/bleujin").children().toList();
			if (children.size() > 0){
				Debug.line(children.size()) ;
//				Debug.line(session.workspace().central().newSearcher().search("").totalCount()) ;
//				session.pathBy("/bleujin").childQuery("").find().debugPrint() ;
			}
			
			Thread.sleep(1000) ;
		}
	}
	
	
	public void testRandomWrite() throws Exception {
		while(true){
//			ReadNode find = session.pathBy("/bleujin/" + RandomUtil.nextInt(15000));
			
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/bleujin/" + RandomUtil.nextInt(100) + 15000).property("index", "ddd");
					return null;
				}
			}) ;
			Thread.sleep(100) ;
		}
	}
	
	
	
	public void testRead() throws Exception {
		session.ghostBy("/bleujin").children().debugPrint() ;
	}
	
	
}
