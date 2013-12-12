package net.ion.script;

import net.ion.bleujin.infinispan.TestReadAtOther;
import net.ion.script.rhino.TestRhinoBinding;
import net.ion.script.rhino.TestJavascriptFunction;
import net.ion.script.rhino.TestOnCraken;
import net.ion.script.rhino.TestRhinoResponse;
import net.ion.script.rhino.TestRhinoReturn;
import net.ion.script.rhino.TestRhinoScript;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllRhino extends TestCase {

	public static TestSuite suite(){
		TestSuite result = new TestSuite() ;
		
		result.addTestSuite(TestRhinoScript.class) ;
		result.addTestSuite(TestRhinoBinding.class) ;
		result.addTestSuite(TestRhinoResponse.class) ;
		result.addTestSuite(TestRhinoReturn.class) ;

		result.addTestSuite(TestJavascriptFunction.class) ;
		result.addTestSuite(TestOnCraken.class) ;
		
		
		return result ;
	}
}
