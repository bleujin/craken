package net.ion.craken.tree;

import java.io.File;

import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.FileUtil;
import junit.framework.TestCase;

public class TestTreeNodeDocument extends TestCase {
	
	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
//		FileUtil.deleteDirectory(new File("./resource/index")) ;
		r.defineWorkspace("test", CentralCacheStoreConfig.create().maxNodeEntry(5)) ;
		this.session = r.login("test");
	}

	public void testInit() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 3)) ;
		assertEquals(true, session.exists("/bleujin/1")) ;
		
		session.pathBy("/bleujin").childrenNames() ; // load
		
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin/0").property("other", "bleujin") ;
				wsession.pathBy("/bleujin/100").property("name", "new");
//				wsession.pathBy("/bleujin").addChild("100").property("name", "new");
				return null;
			}
		}) ;
		assertEquals(4, session.pathBy("/bleujin").childrenNames().size()) ;
	}
	
	public void testReload() throws Exception {
		session.pathBy("/bleujin").children().debugPrint() ;
		assertEquals(4, session.pathBy("/bleujin").childrenNames().size()) ;
	}
	
	
	public void tearDown() throws Exception {
		r.shutdown() ;
	}

}
