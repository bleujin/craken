package net.ion.craken.node.crud;

import net.ion.craken.node.crud.property.TestAppend;
import net.ion.craken.node.crud.property.TestBasicType;
import net.ion.craken.node.crud.property.TestInnerChild;
import junit.framework.TestSuite;

public class TestAllCrudNode extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All CrakenNode") ;
		suite.addTestSuite(TestFirst.class) ;
		suite.addTestSuite(TestListener.class) ;
		suite.addTestSuite(TestPathBy.class) ;
		suite.addTestSuite(TestReadNode.class) ;
		suite.addTestSuite(TestReadSession.class) ;
		suite.addTestSuite(TestTransaction.class) ;
		suite.addTestSuite(TestWriteNode.class) ;
		suite.addTestSuite(TestWriteSession.class) ;
		
		suite.addTestSuite(TestBasicType.class) ;
		suite.addTestSuite(TestInnerChild.class) ;
		suite.addTestSuite(TestAppend.class) ;
		
		return suite ;
	}
	
}
