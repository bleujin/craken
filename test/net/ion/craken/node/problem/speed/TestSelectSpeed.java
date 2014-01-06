package net.ion.craken.node.problem.speed;

import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.loaders.EntryKey;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.loaders.lucene.DocEntry;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.problem.store.SampleWriteJob;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.infinispan.container.entries.InternalCacheEntry;

public class TestSelectSpeed extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/select"));
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
		session.tranSync(new SampleWriteJob(500000));
		Debug.line(System.currentTimeMillis() - start);
	}

	public void testSearchTest() throws Exception {
		Searcher searcher = session.workspace().central().newSearcher();
		final SearchResponse response = searcher.createRequest(new TermQuery(new Term(EntryKey.PARENT, "/"))).selections(IKeywordField.ISKey).offset(1000000).find();
		List<ReadDocument> docs = response.getDocument();
		InternalCacheEntry entry = DocEntry.create(TreeNodeKey.fromString(""), docs);
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
