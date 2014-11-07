package net.ion.craken.io;

import java.util.Set;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.TransactionLog;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.tree.PropertyId;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.common.IKeywordField;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.infinispan.Cache;

public class TestLogLob extends TestCase {


	private ReadSession session;


	@Override
	protected void setUp() throws Exception {
		super.setUp();
		RepositoryImpl r = RepositoryImpl.create() ;
		r.defineWorkspace("search") ;
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
