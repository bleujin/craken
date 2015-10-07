package net.ion.bleujin.working;

import java.util.concurrent.Executors;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

public class TestConcurrent extends TestCase {
	
	public void testRun() throws Exception {
		Craken craken = Craken.local() ;
//		craken.createWorkspace("test", WorkspaceConfigBuilder.memoryDir()) ;
		craken.createWorkspace("test", WorkspaceConfigBuilder.indexDir("")) ;
		
		ReadSession session = craken.login("test") ;
		session.workspace().executorService(Executors.newCachedThreadPool()) ;
		
		final String[] names = new String[]{"bleujin", "hero", "jin"};
		
		
		for (int i = 0; i < 20 ; i++) {
			final String name = names[RandomUtil.nextRandomInt(2)];
			
			if (session.exists("/schedule/" + name)) {
				Debug.line(name + " canceled");
				continue ;
			}
			
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/schedule", name).property("name", name) ;
					Debug.line(name + " started");
					return null;
				}
			}).get() ;
			
			session.tran(new TransactionJob<Void>() {
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					Thread.sleep(100 + RandomUtil.nextInt(1000));
//					Debug.line(name); 
					wsession.pathBy("/schedule_count", name).increase("count") ;
					wsession.pathBy("/schedule", name).removeSelf() ;
					Debug.line(name + " end");
					return null;
				}
			}) ;
			
			Thread.sleep(100 + RandomUtil.nextInt(300));
			
		}
		
		Thread.sleep(2000);
		craken.login("test").ghostBy("/schedule_count").children().debugPrint();
		craken.login("test").ghostBy("/schedule").children().debugPrint();
		craken.stop();
	}
}
