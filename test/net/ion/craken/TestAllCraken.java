package net.ion.craken;

import junit.framework.TestSuite;
import net.ion.craken.node.TestAllChildren;
import net.ion.craken.node.TestAllWorkspace;
import net.ion.craken.node.TestIndexWriteConfig;
import net.ion.craken.node.TestSessionTracer;
import net.ion.craken.node.convert.TestAllConvert;
import net.ion.craken.node.crud.TestAllCrudNode;
import net.ion.craken.node.mr.TestAllMapReduce;
import net.ion.craken.node.ref.TestAllRef;
import net.ion.craken.node.search.TestAllSearchNode;

public class TestAllCraken extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All CrakenNode") ;
		
		// beginner
		suite.addTest(TestAllCrudNode.suite()) ;
		suite.addTest(TestAllRef.suite()) ;
		suite.addTest(TestAllChildren.suite()) ;
		suite.addTest(TestAllWorkspace.suite()) ;

		// intermediate
		suite.addTest(TestAllSearchNode.suite()) ;
		suite.addTest(TestAllConvert.suite()) ;

		
		// advanced
		suite.addTest(TestAllMapReduce.suite()) ;
		
		
		// etc
		suite.addTestSuite(TestIndexWriteConfig.class);
		suite.addTestSuite(TestSessionTracer.class);
		return suite ;
	}
	
}
