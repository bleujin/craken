package net.ion.craken.node.search;

import net.ion.craken.node.crud.property.TestBasicType;
import net.ion.craken.node.crud.property.TestInnerChild;
import junit.framework.TestSuite;

public class TestAllSearchNode extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All CrakenNode") ;
		suite.addTestSuite(TestFirst.class) ;

		suite.addTestSuite(TestFilter.class) ;
		suite.addTestSuite(TestIndex.class) ;
		suite.addTestSuite(TestPaging.class) ;
		suite.addTestSuite(TestReIndex.class) ;
		suite.addTestSuite(TestSearch.class) ;

		
		
		return suite ;
	}
	
}
