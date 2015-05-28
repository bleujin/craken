package net.ion.bleujin.craken.oom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.store.CrakenWorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;

public class TestSingleWorkspace extends TestBaseWorkspace {
	
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
		FileUtil.deleteDirectory(new File("./resource/single"));
		
		craken.createWorkspace("single", CrakenWorkspaceConfigBuilder.singleDir("./resource/single")) ;
		
		ReadSession session = craken.login("single");
		session.tran(makeJob(20000));
	}

	public void testIndexConfirm() throws Exception {
		craken.createWorkspace("single", CrakenWorkspaceConfigBuilder.singleDir("./resource/single")) ;
		
		ReadSession session = craken.login("single");
		Debug.line(session.root().childQuery("", true).offset(10000).find().size()) ;
	}

	
	public void testWrite() throws Exception {
		craken.createWorkspace("single", CrakenWorkspaceConfigBuilder.singleDir("./resource/single")) ;
		ReadSession session = craken.login("single");
		session.tran(TransactionJobs.HelloBleujin) ;
	}

	public void testRead() throws Exception {
		craken.createWorkspace("single", CrakenWorkspaceConfigBuilder.singleDir("./resource/single")) ;
		ReadSession session = craken.login("single");
		ReadNode found = session.pathBy("/bleujin");
		found.debugPrint();
		
		assertEquals("bleujin", found.property("name").asString());
		assertEquals(20, found.property("age").asInt());
	}

	public void testWriteBlob() throws Exception {
		craken.createWorkspace("single", CrakenWorkspaceConfigBuilder.singleDir("./resource/single")) ;
		ReadSession session = craken.login("single");
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
}
