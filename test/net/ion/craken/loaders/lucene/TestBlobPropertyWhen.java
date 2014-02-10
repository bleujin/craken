package net.ion.craken.loaders.lucene;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.infinispan.Cache;

import junit.framework.TestCase;
import net.ion.craken.io.GridBlob;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

public class TestBlobPropertyWhen extends TestCase  {

	private RepositoryImpl r;
	private ReadSession session;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		this.session = r.login("test") ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				InputStream input = new ByteArrayInputStream("Hello".getBytes()) ;
				wsession.pathBy("/bleujin").property("name", "bleujin").blob("blob", input) ;
				return null;
			}
		}) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}

	public void testTwiceRead() throws Exception {
		GridBlob blob = session.pathBy("/bleujin").property("blob").asBlob();
		assertEquals(5, blob.getMetadata().getLength());
		assertEquals("Hello", IOUtil.toStringWithClose(blob.toInputStream())) ;
		assertEquals("Hello", IOUtil.toStringWithClose(blob.toInputStream())) ;
	}
	
	public void testWhenUnset() throws Exception {
		GridBlob blob = session.pathBy("/bleujin").property("blob").asBlob();
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").unset("blob") ;
				return null;
			}
		}) ;
		
		assertEquals(true, blob.toInputStream().available() <= 0) ;
		assertEquals(0, session.workspace().gfs().cacheData().size()) ; 
	}
	
	
	public void testWhenClear() throws Exception {
		GridBlob blob = session.pathBy("/bleujin").property("blob").asBlob();
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").clear() ;
				return null;
			}
		}) ;
		
		assertEquals("", IOUtil.toStringWithClose(blob.toInputStream()));
		assertEquals(true, blob.toInputStream().available() <= 0) ;
		
		Cache<String, byte[]> cache = session.workspace().gfs().cacheData() ;
		assertEquals(0, session.workspace().gfs().cacheData().size()) ; 
	}
	
	
	public void testWhenRemoveSelf() throws Exception {
		GridBlob blob = session.pathBy("/bleujin").property("blob").asBlob();
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").removeSelf() ;
				return null;
			}
		}) ;
		
		assertEquals("", IOUtil.toStringWithClose(blob.toInputStream()));
		assertEquals(true, blob.toInputStream().available() <= 0) ;
		
		Cache<String, byte[]> cache = session.workspace().gfs().cacheData() ;
		assertEquals(0, session.workspace().gfs().cacheData().size()) ; 
	}
	
	public void testWhenRemoveChild() throws Exception {
		
	}
	
	public void testWhenRemoveChildren() throws Exception {
		
	}
	
	
	
	
	
}	
