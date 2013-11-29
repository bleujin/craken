package net.ion.craken.loaders.lucene;

import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.FileUtil;

public class TestLogWorking extends TestCase {


	private ReadSession session;
	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		final String location = "./resource/commit";
		FileUtil.deleteDirectory(new File(location)) ;
		this.r = RepositoryImpl.create();
//		r.defineWorkspace("test", CentralCacheStoreConfig.createDefault().location(location)) ;
		r.defineWorkspaceForTest("test", CentralCacheStoreConfig.createDefault().location(location)) ;
		this.session = r.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}

	
	public void testRun() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.tranId("12345") ;
				wsession.createBy("/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/bleujin").property("name", "hero").property("age", 20)  ; // .blob("blob", new StringInputStream("Long Long String"))  ;
				return null;
			}
		}) ;
		
		assertEquals("hero", session.pathBy("/bleujin").property("name").stringValue()) ;
		
//		Debug.line(IOUtil.toStringWithClose(tranInput)) ;
		
	}
}
