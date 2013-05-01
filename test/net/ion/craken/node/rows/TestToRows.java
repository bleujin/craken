package net.ion.craken.node.rows;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.FilteredQuery;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.similar.MoreLikeThisQuery;
import org.apache.lucene.util.Version;
import org.infinispan.lucene.IndexScopedKey;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.search.TestBaseSearch;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.nsearcher.common.IKeywordField;

public class TestToRows extends TestBaseSearch {
	

	public void testFirst() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				for (int i : ListUtil.rangeNum(100)) {
					wsession.root().addChild("/board1/" +  i).property("index", i).property("name", "board1").property("writer", "") ;
				}
				for (int i : ListUtil.rangeNum(100)) {
					wsession.root().addChild("/board2/" +  i).property("index", i).property("name", "board2").property("writer", "") ;
				}
				return null;
			}
		}) ;
		
		
		session.awaitIndex() ;
		
		long start = System.currentTimeMillis() ;
		//Debug.line(session.pathBy("/board").children().toList().size()) ;
		StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		Query query = new QueryParser(Version.LUCENE_CURRENT, IKeywordField.ISALL_FIELD, analyzer).parse(IKeywordField.ISKey + ":/board2/*") ;
		MoreLikeThisQuery mquery = new MoreLikeThisQuery("/board2/", new String[0], analyzer, IKeywordField.ISKey) ;
		Debug.line(query) ;
		// query = new MatchAllDocsQuery() ;// 
		session.createRequest("").filter(new QueryWrapperFilter(query)).descending("index").skip(10).offset(10).find().debugPrint() ;
		
		
		Debug.line(System.currentTimeMillis() - start) ;
		
	}
}
