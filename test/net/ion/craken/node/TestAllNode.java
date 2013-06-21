package net.ion.craken.node;

import net.ion.craken.node.convert.TestAllConvert;
import net.ion.craken.node.crud.TestAllCrudNode;
import net.ion.craken.node.mr.TestMapReduce;
import net.ion.craken.node.ref.TestAllRef;
import net.ion.craken.node.search.TestAllSearchNode;
import junit.framework.TestSuite;

public class TestAllNode extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All CrakenNode") ;
		suite.addTest(TestAllConvert.suite()) ;
		suite.addTest(TestAllCrudNode.suite()) ;
		
		suite.addTestSuite(TestMapReduce.class) ;
		
		suite.addTest(TestAllRef.suite()) ;
		suite.addTest(TestAllSearchNode.suite()) ;

		
		
		return suite ;
	}
	
}
