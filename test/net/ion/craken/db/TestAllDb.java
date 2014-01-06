package net.ion.craken.db;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllDb extends TestCase {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All DbProcedure With Craken") ;

		suite.addTestSuite(TestUserProcedure.class) ;
		suite.addTestSuite(TestUserProcedureBatch.class) ;
		suite.addTestSuite(TestUserProcedures.class) ;
		
		
		return suite ;
	}
	
}
