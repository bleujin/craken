package net.ion.bleujin.craken.oom;

import java.io.File;
import java.io.InputStream;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.Indexer;

import org.infinispan.configuration.cache.CacheMode;

public class TestSifsWorkspace extends TestBaseWorkspace {

	private Craken craken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.create();
	}

	@Override
	protected void tearDown() throws Exception {
		craken.shutdown();
		super.tearDown();
	}

	public void testConfirmOOM() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/sifs"));

		craken.createWorkspace("sifs", WorkspaceConfigBuilder.sifsDir("./resource/sifs"));

		ReadSession session = craken.login("sifs");
		session.tran(makeJob(200000));
	}

	public void testIndexConfirm() throws Exception {
		craken.createWorkspace("sifs", WorkspaceConfigBuilder.sifsDir("./resource/sifs"));

		ReadSession session = craken.login("sifs");
		Debug.line(session.root().childQuery("", true).offset(1000).find().size());
	}

	public void testForceIndex() throws Exception {
		craken.createWorkspace("sifs", WorkspaceConfigBuilder.sifsDir("./resource/sifs"));

		ReadSession session = craken.login("sifs");
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().text("content");
				// wsession.root().index(new CJKAnalyzer(), true) ;
				return null;
			}

		});

	}

	public void testWriteBlob() throws Exception {
		craken.createWorkspace("sifs", WorkspaceConfigBuilder.sifsDir("./resource/sifs"));
		ReadSession session = craken.login("sifs");
		// session.tran(new TransactionJob<Void>(){
		// @Override
		// public Void handle(WriteSession wsession) throws Exception {
		// wsession.pathBy("/bleujin").blob("msg", new StringInputStream("hello world")) ;
		// return null;
		// }
		// }) ;

		InputStream input = session.pathBy("/bleujin").property("msg").asBlob().toInputStream();
		Debug.line(IOUtil.toStringWithClose(input));
	}

	
	public void testViewSearchIndex() throws Exception {
		
		craken.createWorkspace("sifs", WorkspaceConfigBuilder.sifsDir("./resource/sifs"));
		ReadSession session = craken.login("sifs");

		Debug.line(session.root().childQuery("", true).find().totalCount()) ;
	}
	
	public void testIndexDirect() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/sifs"));
		craken.createWorkspace("sifs", WorkspaceConfigBuilder.sifsDir("./resource/sifs"));
		ReadSession session = craken.login("sifs");
		
		Central central = session.workspace().central() ;

		Indexer indexer = central.newIndexer();
		indexer.index(super.makeIndexJob(200000));
	}
	
	public void testDirectSearch() throws Exception {
//			FileUtil.deleteDirectory(new File("./resource/sifs"));
		craken.createWorkspace("sifs", WorkspaceConfigBuilder.sifsDir("./resource/sifs").distMode(CacheMode.DIST_SYNC));
		ReadSession session = craken.login("sifs");
		Central central = session.workspace().central() ;
		Debug.line(central.newSearcher().createRequest("").find().totalCount()) ;
		new InfinityThread().startNJoin(); 
	}

	
}
