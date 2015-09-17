package net.ion.craken.node.crud;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.infinispan.configuration.cache.CacheMode;

import net.ion.craken.listener.AsyncCDDHandler;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.store.MemoryWorkspaceBuilder;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.InfinityThread;
import junit.framework.TestCase;

public class TestCDDDist extends TestCase {

	public void testRunA() throws Exception {
		System.setProperty("log4j.configuration", new File("./resource/log4j.properties").toURI().toString()) ;
//		System.setProperty("java.net.preferIPv4Stack", "true");
		Craken craken = runServer();


		final ReadSession session = craken.login("cdd") ;
//		session.tran(TransactionJobs.HelloBleujin); 
		
		for (int i = 0; i < 5; i++) {
			final int fi = i ;
			session.tran(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/event/", fi).property("name", "bleujin hi " + fi) ;
					return null;
				}
			});
			Thread.sleep(1000);
		}
		
		new InfinityThread().startNJoin(); 
	}

	private Craken runServer() throws IOException {
		System.setProperty("log4j.configuration", new File("./resource/log4j.properties").toURI().toString()) ;
		Craken c = Craken.create();
		c.createWorkspace("cdd", WorkspaceConfigBuilder.memoryDir().distMode(CacheMode.REPL_SYNC)) ;
		c.start() ;
		
		ReadSession session = c.login("cdd");
		session.workspace().cddm().add(new CDDHandler() {
			
			@Override
			public String pathPattern() {
				return "/event/{eventid}";
			}
			
			@Override
			public TransactionJob<Void> modified(final Map<String, String> resolveMap, final CDDModifiedEvent event) {
				Debug.line(resolveMap.get("eventid"), event.property("name").asString());
				return null;
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				// TODO Auto-generated method stub
				return null;
			}
		}) ;
		return c;
	}
}
