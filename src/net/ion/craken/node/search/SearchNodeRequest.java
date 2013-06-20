package net.ion.craken.node.search;

import java.io.IOException;

import net.ion.craken.node.ReadNode;
import net.ion.craken.tree.Fqn;
import net.ion.framework.db.Page;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.Searcher;
import net.ion.nsearcher.search.filter.FilterUtil;
import net.ion.nsearcher.search.filter.TermFilter;

import org.apache.ecs.xml.XML;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TermRangeFilter;

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

	public SearchNodeRequest refTo(String refName, Fqn target) throws ParseException {
		searcher.andFilter(new TermFilter("@" + refName, target.toString())) ;
		return this ;
	}
	
	
	

	public SearchNodeRequest between(String field, int min, int max) {
		return between(field, 1L * min, 1L * max);
	}

	public SearchNodeRequest between(String field, long min, long max) {
		filter(NumericRangeFilter.newLongRange(field, min, max, true, true));
		return this;
	}

	public SearchNodeRequest between(String field, double min, double max) {
		filter(NumericRangeFilter.newDoubleRange(field, min, max, true, true));
		return this;
	}

	public SearchNodeRequest between(String field, String minTerm, String maxTerm) {
		filter(FilterUtil.and(TermRangeFilter.Less(field, maxTerm), TermRangeFilter.More(field, minTerm)));
		return this;
	}

	public SearchNodeRequest lt(String field, int max) {
		return lt(field, 1L * max);
	}

	public SearchNodeRequest lt(String field, long max) {
		filter(NumericRangeFilter.newLongRange(field, Long.MIN_VALUE, max, true, false));
		return this;
	}

	public SearchNodeRequest lt(String field, double max) {
		filter(NumericRangeFilter.newDoubleRange(field, Double.MIN_VALUE, max, true, false));
		return this;
	}

	public SearchNodeRequest lte(String field, String max) {
		filter(TermRangeFilter.Less(field, max));
		return this;
	}

	public SearchNodeRequest lte(String field, int max) {
		return lte(field, 1L * max);
	}

	public SearchNodeRequest lte(String field, long max) {
		filter(NumericRangeFilter.newLongRange(field, Long.MIN_VALUE, max, true, true));
		return this;
	}

	public SearchNodeRequest lte(String field, double max) {
		filter(NumericRangeFilter.newDoubleRange(field, Double.MIN_VALUE, max, true, true));
		return this;
	}

	public SearchNodeRequest gt(String field, int min) {
		return gt(field, 1L * min);
	}

	public SearchNodeRequest gt(String field, long min) {
		filter(NumericRangeFilter.newLongRange(field, min, Long.MAX_VALUE, false, true));
		return this;
	}

	public SearchNodeRequest gt(String field, double min) {
		filter(NumericRangeFilter.newDoubleRange(field, min, Double.MAX_VALUE, false, true));
		return this;
	}

	public SearchNodeRequest gte(String field, String min) {
		filter(TermRangeFilter.More(field, min));
		return this;
	}

	public SearchNodeRequest gte(String field, int min) {
		return gte(field, 1L * min);
	}

	public SearchNodeRequest gte(String field, long min) {
		filter(NumericRangeFilter.newLongRange(field, min, Long.MAX_VALUE, true, true));
		return this;
	}

	public SearchNodeRequest gte(String field, double min) {
		filter(NumericRangeFilter.newDoubleRange(field, min, Double.MAX_VALUE, true, true));
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
		Filter compositeFilter = (request.getFilter() == null) ? filter : FilterUtil.and(request.getFilter(), filter) ;
		request.setFilter(compositeFilter) ;
		return this ;
	}

	public Filter getFilter() {
		return request.getFilter();
	}

	public SearchNodeResponse find() throws IOException, ParseException{
		request.selections(IKeywordField.ISKey) ;
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
