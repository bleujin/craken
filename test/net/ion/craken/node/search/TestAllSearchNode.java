package net.ion.craken.node.search;

import junit.framework.TestSuite;

public class TestAllSearchNode extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All Node Search") ;
		suite.addTestSuite(TestFirst.class) ;

		suite.addTestSuite(TestFilter.class) ;
		suite.addTestSuite(TestIndex.class) ;
		suite.addTestSuite(TestPaging.class) ;
//		suite.addTestSuite(TestReIndex.class) ;
		suite.addTestSuite(TestSearch.class) ;
		suite.addTestSuite(TestStoreSearch.class) ;
		
		
		return suite ;
	}
	
}
