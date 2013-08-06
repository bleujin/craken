package net.ion.craken.node.crud;

import java.io.IOException;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.db.Page;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.Searcher;
import net.ion.nsearcher.search.filter.FilterUtil;
import net.ion.nsearcher.search.filter.TermFilter;

import org.apache.ecs.xml.XML;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

public class ChildQueryRequest {

	private ReadSession session ;
	private Searcher searcher ;
	private SearchRequest request ;
	
	private ChildQueryRequest(ReadSession session, Query query, Searcher searcher) {
		this.session = session ;
		this.searcher = searcher ;
		this.request = searcher.createRequest(query).selections(IKeywordField.ISKey) ;
	}

	public static ChildQueryRequest create(ReadSession session, Searcher searcher, Query query) {
		return new ChildQueryRequest(session, query, searcher);
	}


	public ChildQueryRequest refTo(String refName, Fqn target) throws ParseException {
		filter(new TermFilter("@" + refName, target.toString())) ;
		return this ;
	}
	
	public ChildQueryRequest in(String field, String... values) {
		if (values == null || values.length == 0) return this ;
		
		filter(Filters.in(field, values)) ;
		return this ;
	}
	

	public ChildQueryRequest between(String field, int min, int max) {
		return between(field, 1L * min, 1L * max);
	}

	public ChildQueryRequest between(String field, long min, long max) {
		filter(Filters.between(field, min, max));
		return this;
	}


	public ChildQueryRequest between(String field, String minTerm, String maxTerm) {
		filter(Filters.between(field, minTerm, maxTerm));
		return this;
	}

	
	
	public ChildQueryRequest lt(String field, int max) {
		return lt(field, 1L * max);
	}

	public ChildQueryRequest lt(String field, long max) {
		filter(Filters.lt(field, max));
		return this;
	}

	public ChildQueryRequest lt(String field, String higherTerm) {
		filter(Filters.lt(field, higherTerm));
		return this;
	}

	
	
	
	public ChildQueryRequest lte(String field, String higherTerm) {
		filter(Filters.lte(field, higherTerm));
		return this;
	}

	public ChildQueryRequest lte(String field, int max) {
		return lte(field, 1L * max);
	}

	public ChildQueryRequest lte(String field, long max) {
		filter(Filters.lte(field, max));
		return this;
	}

	
	
	public ChildQueryRequest gt(String field, int min) {
		return gt(field, 1L * min);
	}

	public ChildQueryRequest gt(String field, long min) {
		filter(Filters.gt(field, min));
		return this;
	}

	public ChildQueryRequest gt(String field, String lowerTerm) {
		filter(Filters.gt(field, lowerTerm));
		return this;
	}
	
	
	public ChildQueryRequest gte(String field, String lowerTerm) {
		filter(Filters.gte(field, lowerTerm));
		return this;
	}

	public ChildQueryRequest gte(String field, int min) {
		return gte(field, 1L * min);
	}

	public ChildQueryRequest gte(String field, long min) {
		filter(Filters.gte(field, min));
		return this;
	}

	
	public ChildQueryRequest eq(String field, Object value) {
		filter(Filters.eq(field, value)) ;
		return this;
	}
	
	public ChildQueryRequest ne(String field, String value){
		filter(Filters.ne(field, value)) ;
		return this ;
	}
	

	public ChildQueryRequest where(String fnString) {
		filter(Filters.where(fnString)) ;
		return this ;
	}


	
	public ChildQueryRequest wildcard(String field, Object value) {
		filter(Filters.wildcard(field, value)) ;
		return this;
	}

	public ChildQueryRequest query(String query) throws ParseException {
		filter(Filters.query(session.central().searchConfig(), query)) ;
		return this;
	}

	

	public ChildQueryRequest skip(int skip){
		request.skip(skip) ;
		return this ;
	}
	
	public Query query() {
		return request.query();
	}

	public ChildQueryRequest page(Page page){
		this.skip(page.getStartLoc()).offset(page.getListNum()) ;
		return this ;
	}
	
	public ChildQueryRequest offset(int offset){
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

	public ChildQueryRequest ascending(String field) {
		request.ascending(field) ;
		return this ;
	}

	public ChildQueryRequest descending(String field) {
		request.descending(field) ;
		return this ;
	}


	public void setParam(String key, Object value) {
		request.setParam(key, value);
	}

	public Object getParam(String key) {
		return request.getParam(key);
	}
	
	public ChildQueryRequest filter(Filter filter) {
		Filter compositeFilter = (request.getFilter() == null) ? filter : FilterUtil.and(request.getFilter(), filter) ;
		request.setFilter(compositeFilter) ;
		return this ;
	}

	public Filter getFilter() {
		return request.getFilter();
	}

	public ChildQueryResponse find() throws IOException, ParseException{
		request.selections(IKeywordField.ISKey) ;
		
		final SearchResponse response = searcher.search(request);
		return ChildQueryResponse.create(session, response) ;
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
