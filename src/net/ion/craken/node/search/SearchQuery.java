package net.ion.craken.node.search;

import java.io.IOException;

import net.ion.craken.node.ReadSession;
import net.ion.nsearcher.search.SearchResponse;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.queryParser.ParseException;

public class SearchQuery {

	private ReadSession session ;
	private Searcher searcher ;
	private String queryText = "";
	private SearchQuery(ReadSession session, Searcher searcher) {
		this.session = session ;
		this.searcher = searcher ;
	}

	public static SearchQuery create(ReadSession session, Searcher searcher) {
		return new SearchQuery(session, searcher);
	}

	public SearchQuery parse(String queryText) {
		this.queryText = queryText ;
		return this;
	}

	public SearchResponse find() throws IOException, ParseException {
		return searcher.createRequest(queryText, session.credential().analyzer()).find() ; 
	}
	
	

}
