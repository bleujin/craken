package net.ion.craken.node.search;

import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.craken.node.crud.util.TransactionJobs;

public class TestFunctionQuery extends TestBaseSearch{
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(TransactionJobs.dummyEmp(20)) ;
	}
	
	public void testChildrenQuery() throws Exception {
		session.pathBy("/emp").children().where("this.dummy >= 10 and this.dummy <= 15").debugPrint() ;
		
	}
	
	public void testChildQuery() throws Exception {
		ChildQueryResponse response = session.pathBy("/emp").childQuery("").where("this.dummy >= 10 AND this.dummy <= 20]").find();
	}
	
	
	
}
