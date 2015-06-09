package net.ion.craken.io;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;

import org.infinispan.Cache;

public class TestLogLob extends TestCase {


	private ReadSession session;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Craken r = Craken.create() ;
		r.createWorkspace("search", WorkspaceConfigBuilder.oldDir("")) ;
		this.session = r.login("search") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		session.workspace().repository().shutdown() ;
	}
	
	public void testTranLog() throws Exception {
		session.tran(TransactionJobs.dummy("/users", 10)) ;

		Cache<String, StringBuilder> logs = session.workspace().cache().getCacheManager().getCache("craken-log") ;
		
		for (String key :  logs.keySet()) {
			Debug.line(key, logs.get(key));
		}
	}
	
	
	public void testRead() throws Exception {
		session.pathBy("/users").children().debugPrint(); 
	}
	
	public void testViewTran() throws Exception {
		Cache<String, StringBuilder> logs = session.workspace().cache().getCacheManager().getCache("craken-log") ;
		
		for (String key :  logs.keySet()) {
			Debug.line(key, logs.get(key));
		}
	}
	
	
}
