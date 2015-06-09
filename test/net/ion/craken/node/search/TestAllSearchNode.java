package net.ion.craken.node.search;

import junit.framework.TestSuite;
import net.ion.craken.node.crud.TestFieldIndexConfig;

public class TestAllSearchNode extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All TreeNode Search") ;
		suite.addTestSuite(TestFirstSearch.class) ;

		suite.addTestSuite(TestFieldIndexConfig.class) ;
		suite.addTestSuite(TestFilter.class) ;
		suite.addTestSuite(TestResponsePredicate.class) ;
		suite.addTestSuite(TestIndex.class) ;
		suite.addTestSuite(TestReIndex.class) ;
		suite.addTestSuite(TestSearch.class) ;
		suite.addTestSuite(TestSearchChild.class) ;
		suite.addTestSuite(TestStoreSearch.class) ;
		suite.addTestSuite(TestAnalyzer.class) ;
		
		return suite ;
	}
	
}
