package net.ion.craken.aradon;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllCrakenServer extends TestCase {

	
	public static TestSuite suite(){
		TestSuite result = new TestSuite() ;
		result.addTestSuite(TestParameterMap.class) ;
		result.addTestSuite(TestTalkEngine.class) ;

		result.addTestSuite(TestScriptLet.class) ;
		result.addTestSuite(TestScriptTalkHandler.class) ;
		return result ;
	}
}
