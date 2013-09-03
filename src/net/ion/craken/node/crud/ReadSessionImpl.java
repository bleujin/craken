package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import net.ion.craken.node.AbstractReadSession;
import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Workspace;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.search.Searcher;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;


public class ReadSessionImpl extends AbstractReadSession{

	private Analyzer queryAnalyzer ;
	
	public ReadSessionImpl(Credential credential, Workspace workspace, Analyzer queryAnalyzer) {
		super(credential, workspace) ;
		this.queryAnalyzer = queryAnalyzer ;
	}

	@Override
	public ReadSessionImpl awaitListener() throws InterruptedException, ExecutionException {
		
		return this;
	}

	@Override
	public Searcher newSearcher() throws IOException {
		return central().newSearcher();
	}


	@Override
	public <T> T indexInfo(IndexInfoHandler<T> indexInfo) {
		return indexInfo.handle(this, central().newReader());
	}

	public Central central() {
		return workspace().central();
	}

	
	@Override
	public ChildQueryRequest queryRequest(String query) throws IOException, ParseException {
		return root().childQuery(query, true);
	}

	@Override
	public Analyzer queryAnalyzer() {
		return queryAnalyzer;
	}

	@Override
	public ReadSession queryAnayzler(Analyzer analyzer) {
		this.queryAnalyzer = analyzer ;
		return this;
	}
}
