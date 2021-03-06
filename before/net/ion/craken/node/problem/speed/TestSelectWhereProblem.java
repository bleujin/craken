package net.ion.craken.node.problem.speed;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.craken.node.problem.store.SampleWriteJob;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

public class TestSelectWhereProblem extends TestCase {

	private Craken r;
	private ReadSession session;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = Craken.create();
		r.createWorkspace("test", OldFileConfigBuilder.directory("./resource/store/select2"));
		r.start();
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	public void testCreateWhenEmpty() throws Exception {
		long start = System.currentTimeMillis();
		session.tranSync(new SampleWriteJob(30000));
		Debug.line(System.currentTimeMillis() - start);
	}

	// 1 sec 10k when full search
	public void testWhereSpeed() throws Exception {
		session.root().children().offset(1).where("id = '/m/0hxhkfn'").debugPrint(); 
	}
	
	public void testTermQuery() throws Exception {
		Searcher searcher = session.workspace().central().newSearcher() ;
		searcher.createRequest(new TermQuery(new Term("id", "/m/0hxhkfn"))).find().debugPrint(); 
	}

	public void testParseQuery() throws Exception {
		Searcher searcher = session.workspace().central().newSearcher() ;
		searcher.createRequest("id:/m/0hxhkfn").find().debugPrint(); 
		Debug.line(); 
		searcher.createRequest("0hxhkfn").find().debugPrint(); 

	}

	
	public void testQueryWhereSpeed() throws Exception {
		Searcher searcher = session.workspace().central().newSearcher() ;
		searcher.createRequest("id:/m/ IS-all:0hxhkfn").find().debugPrint();
		
		Debug.line(searcher.createRequest("/m/0hxhkfn").query());
	}
	
	
	public void testQuery() throws Exception {
		Searcher searcher = session.workspace().central().newSearcher() ;
		Debug.line(searcher.createRequest(new TermQuery(new Term("id", "/m/0hxhkfn"))).query());
		
	}
	
	
	
	
	
}
