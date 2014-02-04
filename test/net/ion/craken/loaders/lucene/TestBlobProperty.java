package net.ion.craken.loaders.lucene;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import junit.framework.TestCase;
import net.ion.craken.io.GridBlob;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;

public class TestBlobProperty extends TestCase  {

	private RepositoryImpl r;
	private ReadSession session;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testBlobWriteInISearcher() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/local")) ;

		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/local")) ;
		r.start() ;
		
		
		session = r.login("test") ;
		
		final InputStream input = new ByteArrayInputStream("HelloWorld".getBytes()) ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.tranId("myid") ;
				wsession.pathBy("/bleujin").blob("img", input) ;
				return null;
			}
		}) ;
		
		
		GridBlob blob = session.pathBy("/bleujin").property("img").asBlob();
		Debug.line(blob);
		
		// session.workspace().gfs().gridBlob("/bleujin/img", metadata)
	}
	
	
	public void testRead() throws Exception {
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/local")) ;
		r.start() ;
		
		
		session = r.login("test") ;
		GridBlob blob = session.pathBy("/bleujin").property("img").asBlob();
		Debug.line(blob);
		
	}
	
	
	
}
