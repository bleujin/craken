package net.ion.craken.node.crud;

import junit.framework.TestSuite;

public class TestAllCrudNode extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All CrakenNode") ;
		suite.addTestSuite(TestFirst.class) ;
		suite.addTestSuite(TestPathBy.class) ;
		suite.addTestSuite(TestReadNode.class) ;
		suite.addTestSuite(TestTransaction.class) ;
		suite.addTestSuite(TestWriteNode.class) ;
		
		return suite ;
	}
	
}
