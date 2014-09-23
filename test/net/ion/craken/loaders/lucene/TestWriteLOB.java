package net.ion.craken.loaders.lucene;

import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import net.ion.craken.io.GridBlob;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import org.infinispan.manager.DefaultCacheManager;

import junit.framework.TestCase;

public class TestWriteLOB extends TestCase {

	
	private RepositoryImpl r;
	private ReadSession session;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.test(new DefaultCacheManager(), "test");
		r.defineWorkspaceForTest("twork", ISearcherWorkspaceConfig.create().location("./resource/test"));
		r.start();
		
		this.session = r.login("twork") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	
	public void testWrite() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				ByteArrayInputStream binput = new ByteArrayInputStream("Hello".getBytes());
				wsession.pathBy("/blob").property("name", "bleujin").blob("blob", binput) ;
				return null;
			}
		}) ;
		
		
		tearDown();
		setUp(); 

		printBlob(session.pathBy("/blob") );
	}

	private void printBlob(ReadNode found) throws FileNotFoundException, IOException {
		GridBlob blob = found.property("blob").asBlob() ;
		
		InputStream input = blob.toInputStream() ;
		Debug.line(blob.getMetadata());
		Debug.line(IOUtil.toStringWithClose(input));
	}
}
