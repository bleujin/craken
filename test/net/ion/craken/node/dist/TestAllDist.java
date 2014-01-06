package net.ion.craken.node.dist;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllDist extends TestCase{

	
	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All Dist");
		
		suite.addTestSuite(LoadAfterStop.class) ;
		suite.addTestSuite(TestAtSingleCom.class) ;
		
		return suite ;
	}

}
