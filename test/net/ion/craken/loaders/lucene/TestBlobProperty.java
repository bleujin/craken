package net.ion.craken.loaders.lucene;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.InputStream;

import junit.framework.TestCase;
import net.ion.craken.io.GridBlob;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;

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
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				InputStream input = new ByteArrayInputStream("Hello World".getBytes()) ;
				wsession.tranId("myid") ;
				wsession.pathBy("/bleujin").blob("img", input) ;
				input.close(); 
				return null;
			}
		}) ;
		
		
		PropertyValue pvalue = session.pathBy("/bleujin").property("img");
		assertEquals(true, pvalue.isBlob());
		GridBlob blob = pvalue.asBlob();
		
		Debug.line(blob);
		
		// session.workspace().gfs().gridBlob("/bleujin/img", metadata)
	}
	
	
	public void testRead() throws Exception {
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/local")) ;
		r.start() ;
		
		session = r.login("test") ;
		GridBlob blob = session.pathBy("/bleujin").property("img").asBlob();
		Debug.line(blob);
		Debug.line(IOUtil.toStringWithClose(blob.toInputStream())) ; 
		
	}
	
	
	public void testBigSizeWrite() throws Exception {
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/local")) ;
		r.start() ;
		
		session = r.login("test") ;

		final File file = File.createTempFile("craken", "blob");
		FileWriter fw = new FileWriter(file) ;
		for (int i = 0; i < 100000; i++) {
			fw.write("0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ"); // about 5-6m
		}
		fw.close(); 
		
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				FileInputStream fis = new FileInputStream(file) ;
				wsession.pathBy("/blob").blob("content", fis) ;
				return null;
			}
		}).get() ;
	}
	
	

	public void testBigSizeRead() throws Exception {
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/local")) ;
		r.start() ;
		
		session = r.login("test") ;
		GridBlob blob = session.pathBy("/blob").property("content").asBlob();
		Debug.line(blob);
		InputStream input = blob.toInputStream() ;
		byte[] buffer = new byte[512] ;
		
		int readed = 0 ;
		int sumByte = 0 ;
		while(true ){
			readed = input.read(buffer) ;
			sumByte += readed ;
			if (readed < 512) break ;
		} 
		
		Debug.line(sumByte);
	}

	
	
	
	
	
}
