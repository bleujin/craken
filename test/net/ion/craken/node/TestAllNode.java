package net.ion.craken.node;

import junit.framework.TestSuite;
import net.ion.craken.node.convert.TestAllConvert;
import net.ion.craken.node.crud.TestAllCrudNode;
import net.ion.craken.node.dist.TestAllDist;
import net.ion.craken.node.mr.TestAllMapReduce;
import net.ion.craken.node.ref.TestAllRef;
import net.ion.craken.node.search.TestAllSearchNode;

public class TestAllNode extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All CrakenNode") ;
		
		// beginner
		suite.addTest(TestAllCrudNode.suite()) ;
		suite.addTest(TestAllRef.suite()) ;

		// intermediate
		suite.addTest(TestAllSearchNode.suite()) ;
		suite.addTest(TestAllConvert.suite()) ;

		
		// advanced
		suite.addTest(TestAllDist.suite()) ;
		suite.addTest(TestAllMapReduce.suite()) ;
		
		return suite ;
	}
	
}
