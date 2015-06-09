package net.ion.craken.node.crud;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

public class RefQueryRequest extends ChildQueryRequest {

	protected RefQueryRequest(ReadSession session, Query query, Searcher searcher) {
		super(session, query, searcher);
	}

	public static RefQueryRequest createMe(ReadSession session, Searcher searcher, Fqn fqn, String refName) {
		TermQuery query = new TermQuery(new Term('@' + refName, fqn.toString())) ;
		return new RefQueryRequest(session, query, searcher);
	}

	public static RefQueryRequest createChildren(ReadSession session, Searcher searcher, Fqn fqn, String refName) {
		WildcardQuery query = new WildcardQuery(new Term('@' + refName, fqn.toString() + "/*")) ;
		return new RefQueryRequest(session, query, searcher);
	}


	public RefQueryRequest fqnFilter(String path){
		this.filter(new QueryWrapperFilter(Fqn.fromString(path).childrenQuery())) ;
		return this ;
	}

}
