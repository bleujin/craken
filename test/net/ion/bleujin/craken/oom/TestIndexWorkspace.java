package net.ion.bleujin.craken.oom;

import java.io.File;
import java.io.InputStream;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.search.Searcher;

import org.infinispan.configuration.cache.CacheMode;

public class TestIndexWorkspace extends TestBaseWorkspace {
	
	private Craken craken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.create();
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.shutdown() ;
		super.tearDown();
	}
	
	public void testConfirmOOM() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/index"));
		
		craken.createWorkspace("index", WorkspaceConfigBuilder.indexDir("./resource/index")) ;
		
		ReadSession session = craken.login("index");
		session.tran(makeJob(20000));
	}

	public void testIndexConfirm() throws Exception {
		craken.createWorkspace("index", WorkspaceConfigBuilder.indexDir("./resource/index")) ;
		
		ReadSession session = craken.login("index");
		Debug.line(session.root().childQuery("", true).offset(50000).find().size()) ;
	}

	
	public void testWrite() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/index"));
		
		craken.createWorkspace("index", WorkspaceConfigBuilder.indexDir("./resource/index")) ;
		ReadSession session = craken.login("index");
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("age", 29).property("firstname", "bleu jin") ;
				return null;
			}
			
		}) ;
		
		session.pathBy("/bleujin").debugPrint();
		
	}

	public void testRead() throws Exception {
		craken.createWorkspace("index", WorkspaceConfigBuilder.indexDir("./resource/index")) ;
		ReadSession session = craken.login("index");
		ReadNode found = session.pathBy("/bleujin");
		found.debugPrint();

		
		Searcher searcher = session.workspace().central().newSearcher() ;
		ReadDocument rdoc = searcher.createRequest("name:bleujin").findOne() ;
		Debug.line("found", rdoc, rdoc.getField("name"), rdoc.getField("age"));

		
		assertEquals("bleujin", found.property("name").asString());
		assertEquals(20, found.property("age").asInt());
	}

	public void testWriteBlob() throws Exception {
		craken.createWorkspace("index", WorkspaceConfigBuilder.indexDir("./resource/index")) ;
		ReadSession session = craken.login("index");
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").blob("msg", new StringInputStream("hello world")) ;
				return null;
			}
		}) ;
		
		InputStream input = session.pathBy("/bleujin").property("msg").asBlob().toInputStream() ;
		Debug.line(IOUtil.toStringWithClose(input));
	}
	
	
	public void testIndexDirect() throws Exception {
//		FileUtil.deleteDirectory(new File("./resource/index"));
		craken.createWorkspace("index", WorkspaceConfigBuilder.indexDir("./resource/index"));
		ReadSession session = craken.login("index");
		
		Central central = session.workspace().central() ;
		
		Debug.line(central.newSearcher().createRequest("").find().totalCount()) ;
		

		Indexer indexer = central.newIndexer();
		indexer.index(super.makeIndexJob(2000));


	}
	
	
	
	public void testIndexLocal() throws Exception {
//		FileUtil.deleteDirectory(new File("./resource/store/index"));
		Central central = CentralConfig.newLocalFile().dirFile("./resource/store/index").indexConfigBuilder().setRamBufferSizeMB(100).setMaxBufferedDocs(500).build() ;
		
		
		Indexer indexer = central.newIndexer();
		indexer.index(super.makeIndexJob(200000));

	}
	
	
	public void testDirectSearch() throws Exception {
//			FileUtil.deleteDirectory(new File("./resource/sifs"));
		craken.createWorkspace("index", WorkspaceConfigBuilder.indexDir("./resource/index").distMode(CacheMode.DIST_SYNC));
		ReadSession session = craken.login("index");
		Central central = session.workspace().central() ;
		central.newSearcher().createRequest("").offset(10).find().debugPrint(); 
		Debug.line(central.newSearcher().createRequest("").find().totalCount()) ;
//		new InfinityThread().startNJoin(); 
	}
}
