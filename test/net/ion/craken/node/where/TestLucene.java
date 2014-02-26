package net.ion.craken.node.where;

import org.apache.lucene.search.FieldValueFilter;

import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.Searcher;
import junit.framework.TestCase;

public class TestLucene extends TestCase {

	
	public void testNotNull() throws Exception {
		Central c = CentralConfig.newRam().build() ;
		c.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
					for (int i = 1000; i < 1010; i++) {
						WriteDocument doc = isession.newDocument("" + i).keyword("name", "bleujin").text("explain", "thinking is high, life is simple") ;
						if (i % 2 == 0) doc.keyword("odded", "true") ;
						isession.insertDocument(doc) ;
					}
				return null;
			}
		}) ;
		
		Searcher searcher = c.newSearcher() ;
		FieldValueFilter filter = new FieldValueFilter("odded") ;
		searcher.createRequest("").setFilter(filter).find().debugPrint(); 
	}
}
