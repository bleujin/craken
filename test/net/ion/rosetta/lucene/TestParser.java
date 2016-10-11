package net.ion.rosetta.lucene;

import junit.framework.TestCase;
import net.ion.craken.node.crud.Filters;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;

public class TestParser extends TestCase{

	public void testQuery() throws Exception {
		
		
		Central central = CentralConfig.newRam().build() ;
		
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.newDocument("/bleujin").keyword("id", "bleujin").number("age", 20).stext("explain", "hello bleujin").update() ;
				isession.newDocument("/hero").keyword("id", "hero").number("age", 25).stext("explain", "hello hero").update() ;
				isession.newDocument("/jin").keyword("id", "jin").number("age", 30).stext("explain", "hello jin").update() ;
				return null;
			}
		}) ;
		
		central.newSearcher().createRequest("").setFilter(Filters.where("(age >= 25) and explain = 'hello' and id in ('jin', 'hero')")).find().debugPrint();
		
	}
}
