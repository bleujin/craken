package net.ion.craken.node.search;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import junit.framework.TestCase;
import net.ion.craken.node.crud.FunctionFilter;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.rosetta.query.Filter;

public class TestFunctionFilter extends TestCase{

	final String andExpr = "dummy >= 10 AND dummy < 20";

	public void testSimple() {
		new FunctionFilter("this.dummy >= 10").create() ;
		new FunctionFilter(andExpr).create() ;
	}

	
	public void testLuceneFilter() throws Exception {
		Query query = new QueryParser(Version.LUCENE_CURRENT, IKeywordField.ISBody, new StandardAnalyzer(Version.LUCENE_CURRENT)).parse("dummy < 20");
		
		Debug.line(query) ;
	}
	
	
}
