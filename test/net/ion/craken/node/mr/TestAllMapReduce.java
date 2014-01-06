package net.ion.craken.node.mr;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllMapReduce extends TestCase{

	
	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All MapReduce");
		
		suite.addTestSuite(TestMapReduce.class) ;
		
		return suite ;
	}
}
