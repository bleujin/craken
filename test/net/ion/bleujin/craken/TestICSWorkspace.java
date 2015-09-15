package net.ion.bleujin.craken;

import java.io.File;

import org.infinispan.configuration.cache.CacheMode;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.GridFileConfigBuilder;
import net.ion.craken.node.crud.store.ICSFileConfigBuilder;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;

public class TestICSWorkspace extends TestCase  {
	private Craken craken;
	private ReadSession session;

	public void testGridWorkspace() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/grid"));
		
		Craken craken = Craken.local();
		craken.createWorkspace("test", new GridFileConfigBuilder("./resource/grid").maxEntry(1000).distMode(CacheMode.LOCAL)) ;
		ReadSession session = craken.login("test");
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/message/hello").property("greeting", "hi") ;
				return null;
			}
		}) ;
		
		session.root().children().debugPrint(); 
		Debug.line();
		session.root().walkChildren().debugPrint();
		craken.shutdown() ;
	}

	public void testICSWorkspace() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/ics"));
		
		Craken craken = Craken.local();
		craken.createWorkspace("test", new ICSFileConfigBuilder("./resource/ics").maxEntry(1000).distMode(CacheMode.LOCAL)) ;
		ReadSession session = craken.login("test");
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/message/hello").property("greeting", "hi") ;
				return null;
			}
		}) ;
		
		session.root().children().debugPrint(); 
		Debug.line();
		session.root().walkChildren().debugPrint();
		craken.shutdown() ;
	}

}
