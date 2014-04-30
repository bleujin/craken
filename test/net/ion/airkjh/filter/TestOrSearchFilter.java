package net.ion.airkjh.filter;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.ics6.core.TestBasePackage;
import net.ion.ics6.filter.OrSearchFilter;

import org.apache.lucene.search.Filter;

public class TestOrSearchFilter extends TestBasePackage {
	
	public void testWildCard() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				// TODO Auto-generated method stub
				wsession.pathBy("/test").property("id", "hello").property("name", "kim");
				wsession.pathBy("/test2").property("id", "hello2").property("name", "airkjh");
				wsession.pathBy("/test3").property("id", "imhello").property("name", "bleujin");
				wsession.pathBy("/test4").property("id", "john").property("name", "Doe");
				return null;
			}}
		);
		
		Filter filter = OrSearchFilter.wildcard("hello", new String[] {"id", "name"});
		ChildQueryResponse response = session.pathBy("/").childQuery("").filter(filter).find();
		
		assertEquals(3, response.size());
	}
	

}
