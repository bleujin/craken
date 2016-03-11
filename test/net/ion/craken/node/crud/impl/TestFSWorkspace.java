package net.ion.craken.node.crud.impl;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.FileStoreWorkspaceConfigBuilder;
import net.ion.craken.node.crud.store.FileSystemWorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectId;

public class TestFSWorkspace extends TestCase{

	private ReadSession rsession;
	private Craken craken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.inmemoryCreateWithTest() ;
		craken.createWorkspace("fs", FileStoreWorkspaceConfigBuilder.test().maxEntry(100)) ;
		this.rsession = craken.login("fs") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.shutdown() ;
		super.tearDown();
	}

	public void testChildren() throws Exception {
		rsession.tran(TransactionJobs.REMOVE_ALL) ;
		final TransactionJob<Void> job = new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 10; i++) {
					wsession.pathBy("/emps/" + new ObjectId().toString()).property("name", "dummy") ;
				}
				return null;
			}
		};
		rsession.tran(job) ;
		
		rsession.root().children().debugPrint(); 
		
		
		for(Object key : rsession.workspace().cache().keySet()){
			Debug.line(key);
		}
		
	}
	
	public void testMaxEntry() throws Exception {
		rsession.tran(TransactionJobs.REMOVE_ALL) ;
		final TransactionJob<Void> job = new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 10; i++) {
					wsession.pathBy("/emps/" + new ObjectId().toString()).property("name", "dummy") ;
				}
				return null;
			}
		};
		for (int i = 0; i < 10; i++) {
			rsession.tran(job) ;
		}
		rsession.root().walkChildren().debugPrint();
		
//		Thread.sleep(30000);
	}

}
