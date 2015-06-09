package net.ion.craken.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import com.sun.corba.se.impl.activation.RepositoryImpl;

public class TestNodeBlob extends TestCase {

	private Craken r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.create() ;
		r.createWorkspace("search", OldFileConfigBuilder.directory("./resource/store/search")) ;
		this.session = r.login("search") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	String targetFqn = "/files/craken-cache-config.xml";
	public void testWriteFile() throws Exception {
		File dir = new File("./resource/config") ;
		for (final File file : dir.listFiles()) {
			if (file.isDirectory()) continue ;
			session.tran(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/files/" + file.getName()).property("filepath", file.getCanonicalPath()).blob("content", new FileInputStream(file)) ;
					return null;
				}
			}) ;
		}
		
		Debug.line(session.pathBy(targetFqn).property("content").asString()) ;
		
		InputStream input = session.pathBy(targetFqn).property("content").asBlob().toInputStream() ;
		String content = IOUtil.toStringWithClose(input) ;
		Debug.line(content);
	}
	
	
	public void testRead() throws Exception {

		InputStream input = session.pathBy(targetFqn).property("content").asBlob().toInputStream() ;
		String content = IOUtil.toStringWithClose(input) ;
		Debug.line(content);
	}
	
	public void testRewrite() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				final WriteNode wnode = wsession.pathBy(targetFqn).property("name", "hello").blob("content", new StringInputStream("LongLongString"));
				return null;
			}
		}) ;
		
		ReadNode readNode = session.pathBy(targetFqn);
		assertEquals("hello", readNode.property("name").stringValue()) ;
		Debug.line(IOUtil.toStringWithClose(readNode.property("content").asBlob().toInputStream())) ;
	}
	

}
