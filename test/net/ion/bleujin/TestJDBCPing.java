package net.ion.bleujin;

import java.util.concurrent.atomic.AtomicInteger;

import org.infinispan.manager.DefaultCacheManager;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestJDBCPing extends TestCase {

	public void testMulticastPingRun() throws Exception {
		Craken craken = Craken.create(new DefaultCacheManager("./resource/config/craken-mping-config.xml"), "emanon") ;
		
		craken.createWorkspace("mem", WorkspaceConfigBuilder.memoryDir()) ;
		
		ReadSession session = craken.login("mem") ;
		session.tran(TransactionJobs.HelloBleujin) ;
		
		session.root().walkChildren().debugPrint(); 
		
		
		craken.shutdown() ;
	}
	
	public void testTCPPing() throws Exception {
		Craken craken = Craken.create(new DefaultCacheManager("./resource/config/craken-tcp-config.xml"), "emanon") ;
		
		craken.createWorkspace("mem", WorkspaceConfigBuilder.memoryDir()) ;
		
		ReadSession session = craken.login("mem") ;
		
		boolean inf = true ;
		final AtomicInteger index = new AtomicInteger() ;
		while(inf){
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/index", index.incrementAndGet()).property("index", index.intValue());
					return null;
				}
			}) ;
			Thread.sleep(1000);
			System.out.print('.');
			Debug.line(session.ghostBy("/index").children().toList().size()) ;
		}
		
		craken.shutdown() ;
	}
}
