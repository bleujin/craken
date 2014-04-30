package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.Iterator;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.db.Page;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.search.SearchRequest;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.Searcher;
import net.ion.nsearcher.search.filter.FilterUtil;
import net.ion.nsearcher.search.filter.MatchAllDocsFilter;
import net.ion.nsearcher.search.filter.TermFilter;

import org.apache.ecs.xml.XML;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;

public class ChildQueryRequest {

	private ReadSession session ;
	private Searcher searcher ;
	private SearchRequest request ;
	private Iterable<String> sorts;
	
	private ChildQueryRequest(ReadSession session, Query query, Searcher searcher) {
		this.session = session ;
		this.searcher = searcher ;
		this.request = searcher.createRequest(query).selections(IKeywordField.ISKey) ;
		this.sorts =  ListUtil.EMPTY ;
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

	public ChildQueryRequest lt(String field, Object max){
		if (max instanceof Long){
			return lt(field, (Long)max) ;
		} else {
			return lt(field, ObjectUtil.toString(max)) ;
		}
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
	public ChildQueryRequest lte(String field, Object max){
		if (max instanceof Long){
			return lte(field, (Long)max) ;
		} else {
			return lte(field, ObjectUtil.toString(max)) ;
		}
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
	public ChildQueryRequest gt(String field, Object max){
		if (max instanceof Long){
			return gt(field, (Long)max) ;
		} else {
			return gt(field, ObjectUtil.toString(max)) ;
		}
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
	public ChildQueryRequest gte(String field, Object max){
		if (max instanceof Long){
			return gte(field, (Long)max) ;
		} else {
			return gte(field, ObjectUtil.toString(max)) ;
		}
	}

	
	public ChildQueryRequest eq(String field, Object value) {
		filter(Filters.eq(field, value)) ;
		return this;
	}
	
	public ChildQueryRequest ne(String field, String value){
		filter(Filters.ne(field, value)) ;
		return this ;
	}
	public ChildQueryRequest ne(String field, Object value){
		filter(Filters.ne(field, ObjectUtil.toString(value))) ;
		return this ;
	}
	

	public ChildQueryRequest where(String fnString) {
		if (StringUtil.isBlank(fnString)) return filter(new MatchAllDocsFilter()) ;
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
		// field=asc && field2=desc...
		request.selections(IKeywordField.ISKey) ;
		
		Iterator<String> iter = sorts.iterator();
		while(iter.hasNext()){
			String[] exp = StringUtil.split(iter.next(), '=') ;
			if (exp.length != 1 && exp.length != 2) throw new IllegalArgumentException("illegal sort expression : "  + exp.toString()) ;
			if (exp.length == 1){
				request.ascending(exp[0]) ;
			} else if (exp.length == 2){
				request = ("desc".equalsIgnoreCase(exp[1])) ? request.descending(exp[0]) : request.ascending(exp[0]) ;
			}
		}
		
		
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

	public ChildQueryRequest sort(String sexpression) {
		this.sorts = Splitter.on('&').trimResults().omitEmptyStrings().split(sexpression) ;
		return this;
	}

}
