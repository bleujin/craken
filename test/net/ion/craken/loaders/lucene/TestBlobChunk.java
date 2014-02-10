package net.ion.craken.loaders.lucene;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import org.infinispan.Cache;

import junit.framework.TestCase;
import net.ion.craken.io.GridBlob;
import net.ion.craken.io.Metadata;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;

public class TestBlobChunk extends TestCase  {

	private RepositoryImpl r;
	private ReadSession session;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}

	
	public void testViewChunk() throws Exception {
		final InputStream input = new FileInputStream(new File("./resource/docs/html.zip"));
//		final InputStream input = new ByteArrayInputStream("Hello.World".getBytes()) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name","bleujin").blob("blob", input) ;
				return null;
			}
		}) ;
		
		GridBlob blob = session.pathBy("/bleujin").property("blob").asBlob() ;
		Metadata meta =  blob.getMetadata() ;
		Debug.line(meta, meta.getLength()) ;
		
		Cache<String, byte[]> cache = session.workspace().gfs().cacheData() ;
		for(String key : cache.keySet()) {
			Debug.line(key, cache.get(key).length);
		}
	}
	
}
