package net.ion.script;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.ion.script.rhino.TestBinding;
import net.ion.script.rhino.TestJavascriptFunction;
import net.ion.script.rhino.TestOnCraken;
import net.ion.script.rhino.TestResponseHandler;
import net.ion.script.rhino.TestReturnType;
import net.ion.script.rhino.TestRhinoScript;
import net.ion.script.rhino.engine.TestTimeOutScript;

public class TestAllRhino extends TestCase {

	public static TestSuite suite(){
		TestSuite result = new TestSuite() ;
		
		result.addTestSuite(TestRhinoScript.class) ;
		result.addTestSuite(TestBinding.class) ;
		result.addTestSuite(TestResponseHandler.class) ;
		result.addTestSuite(TestReturnType.class) ;
		result.addTestSuite(TestContext.class) ;
		

		result.addTestSuite(TestJavascriptFunction.class) ;
		result.addTestSuite(TestImportPackage.class) ;
		result.addTestSuite(TestPreDefineScript.class) ;
		
//		result.addTestSuite(TestTimeOutScript.class) ;
		
		result.addTestSuite(TestOnCraken.class) ;
		
		
		return result ;
	}
}
