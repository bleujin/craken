package net.ion.craken.node.problem.speed;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.loaders.EntryKey;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Filters;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.node.problem.store.SampleWriteJob;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

public class TestSelectSpeed extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
		r.createWorkspace("test", WorkspaceConfigBuilder.directory("./resource/store/select"));
		r.start();
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	// 30sec per 20k(create, about 700 over per sec 6.21, 44M)
	// 18sec per 20k(update, about 1k lower per sec 6.21 70M)
	public void testCreateWhenEmpty() throws Exception {
		long start = System.currentTimeMillis();
		session.tranSync(new SampleWriteJob(100000));
		Debug.line(System.currentTimeMillis() - start);
	}

	// 1 sec 1k when full search
	public void testWhereSpeed() throws Exception {
		session.root().children().offset(1).where("id = '/m/0hxhkfn'").debugPrint(); 
	}
	
	public void testQueryWhereSpeed() throws Exception {
		long start = System.currentTimeMillis() ;
		session.root().childQuery(new TermQuery(new Term("id", "/m/0hxhkfn")), true).find().debugPrint();
		Debug.line(System.currentTimeMillis() - start);
	}
	
	public void testFilterQuery() throws Exception {
		long start = System.currentTimeMillis() ;
		session.root().childQuery("").filter(Filters.where(" (id = '/m/0hxhkfn' || id = '/m/0hxgltr') || section_name = 'Package' ")).find().debugPrint();
		Debug.line(System.currentTimeMillis() - start);
	}
	
	public void testNot() throws Exception {
		long start = System.currentTimeMillis() ;
		session.root().childQuery("").filter(Filters.where(" (not (id = '/m/0hxhkfn' || id = '/m/0hxgltr')) && id = '/m/0hxf_vp' ")).offset(5).find().debugPrint();
		Debug.line(System.currentTimeMillis() - start);
	}
	
	
	
	
	
	
	
	
	
	
	public void testStdSearche() throws Exception {
		Searcher searcher = session.workspace().central().newSearcher();
		final List<ReadDocument> docs = searcher.createRequest("id:/m/0hxhkfn").find().getDocument() ;
		
		
//		Central copied = CentralConfig.newRam().build() ;
//		copied.newIndexer().index(new IndexJob<Void>() {
//			@Override
//			public Void handle(IndexSession isession) throws Exception {
//				for (ReadDocument doc : docs){
//					doc.getFields()
//				}
//				return null;
//			}
//		}) ;
		
		for (ReadDocument doc : docs){
			Debug.debug(doc.bodyValue(), doc.toLuceneDoc()) ;
		}
		
	}
	
	
	public void testSearchTest() throws Exception {
		Searcher searcher = session.workspace().central().newSearcher();
		final SearchResponse response = searcher.createRequest(new TermQuery(new Term(EntryKey.PARENT, "/"))).selections(IKeywordField.DocKey).offset(1000000).find();
		List<ReadDocument> docs = response.getDocument();
		Debug.line(docs.size()) ;
	}
	
	public void testReadStart() throws Exception {
		long start = System.currentTimeMillis() ;
		Iterator<ReadNode> iter = session.root().children().iterator();
//		int i = 0 ;
//		while(iter.hasNext()){
//			ReadNode node = iter.next();
//			i++ ;
//			break ;
//		}
		
//		Debug.line(session.pathBy("/150000").toMap()) ;
		Debug.line(System.currentTimeMillis() - start) ;
	}
}
