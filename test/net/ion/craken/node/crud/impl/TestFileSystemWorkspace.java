package net.ion.craken.node.crud.impl;

import java.io.FileInputStream;
import java.io.InputStream;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.FileSystemWorkspaceConfigBuilder;
import net.ion.craken.node.crud.store.MemoryWorkspaceBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;
import junit.framework.TestCase;

public class TestFileSystemWorkspace extends TestCase {

	private ReadSession rsession;
	private Craken craken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.inmemoryCreateWithTest() ;
		craken.createWorkspace("fs", FileSystemWorkspaceConfigBuilder.test()) ;
//		craken.createWorkspace("fs", MemoryWorkspaceBuilder.icsDir("./resurce/temp")) ;
//		craken.createWorkspace("fs", MemoryWorkspaceBuilder.memoryDir()) ;
		this.rsession = craken.login("fs") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.shutdown() ;
		super.tearDown();
	}

	public void testLoad() throws Exception {
		rsession.root().debugPrint(); 
	}
	
	public void testWrite() throws Exception {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").property("age", 20).property("address", "seoul") ;
				wsession.pathBy("/emps/hero").property("name", "hero").property("age", 22) ;
				return null;
			}
		}) ;
		rsession.root().walkChildren().debugPrint();
		
	}
	
	public void testRead() throws Exception {
		rsession.root().walkChildren().debugPrint();
		assertEquals(20, rsession.ghostBy("/emps/bleujin").property("age").asInt()) ;
	}
	
	public void testDelete() throws Exception {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/").removeChildren() ;
				return null;
			}
		}) ;
		rsession.root().walkChildren().includeSelf(true).debugPrint();
	}
	
	
	public void testWriteBlob() throws Exception {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").blob("pic", new FileInputStream("./resource/helloworld.txt")).property("name", "bleujin") ;
				return null;
			}
		}) ;
		rsession.root().walkChildren().debugPrint();
	}
	
	public void testReadBlob() throws Exception {
		rsession.root().walkChildren().includeSelf(true).debugPrint();
		
		InputStream input = rsession.pathBy("/emps/bleujin").property("pic").asBlob().toInputStream() ;
		Debug.line(IOUtil.toStringWithClose(input));
	}
	
	
	public void testReference() throws Exception {
		rsession.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/").removeChildren() ;
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").refTo("dept", "/dept/dev").refTos("dept", "/dept") ;
				wsession.pathBy("/dept/dev").property("name", "developer") ;
				
				return null;
			}
		}) ;
		
		rsession.pathBy("/emps/bleujin").refs("dept").debugPrint();
	}
	
	
	
	
}
