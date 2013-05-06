package net.ion.craken.node.search;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.ion.craken.node.ReadNode;
import net.ion.craken.tree.Fqn;
import net.ion.framework.db.Page;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.MyDocument;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.Searcher;

import org.apache.ecs.xml.XML;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.similar.MoreLikeThisQuery;
import org.apache.lucene.util.Version;

public class SearchNodeRequest {

	private ReadSearchSession session ;
	private Searcher searcher ;
	private SearchRequest request ;
	
	private SearchNodeRequest(ReadSearchSession session, Query query, Searcher searcher) {
		this.session = session ;
		this.searcher = searcher ;
		this.request = searcher.createRequest(query).selections(IKeywordField.ISKey) ;
	}

	public static SearchNodeRequest create(ReadSearchSession session, Searcher searcher, Query query) {
		return new SearchNodeRequest(session, query, searcher);
	}
	
	
	public SearchNodeRequest belowTo(Fqn topFqn) throws ParseException {
		Query query = searcher.config().parseQuery(IKeywordField.ISKey + ":" + topFqn + "/*");
		
		searcher.andFilter(new QueryWrapperFilter(query)) ;
		return this;
	}

	

	public SearchNodeRequest skip(int skip){
		request.skip(skip) ;
		return this ;
	}
	
	public Query query() {
		return request.query();
	}

	public SearchNodeRequest page(Page page){
		this.skip(page.getStartLoc()).offset(page.getListNum()) ;
		return this ;
	}
	
	public SearchNodeRequest offset(int offset){
		request.offset(offset) ;
		return this ;
	}
	
	public int skip() {
		return request.skip();
	}

	public int offset() {
		return request.offset();
	}

	public Sort sort() {
		return request.sort() ;
	}

	public int limit() {
		return request.limit();
	}

	public SearchNodeRequest ascending(String field) {
		request.ascending(field) ;
		return this ;
	}

	public SearchNodeRequest descending(String field) {
		request.descending(field) ;
		return this ;
	}



	public void setParam(String key, Object value) {
		request.setParam(key, value);
	}

	public Object getParam(String key) {
		return request.getParam(key);
	}
	
	public SearchNodeRequest filter(Filter filter) {
		request.setFilter(filter) ;
		return this ;
	}

	public Filter getFilter() {
		return request.getFilter();
	}

	public SearchNodeResponse find() throws IOException, ParseException{
		return SearchNodeResponse.create(session, searcher.search(request)) ;
	}
	
	public ReadNode findOne() throws IOException, ParseException {
		return find().first() ;
	}


	public XML toXML() {
		return request.toXML() ;
	}

	public String toString() {
		return request.toString() ;
	}

}
