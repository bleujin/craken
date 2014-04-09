package net.ion.craken.node;

import junit.framework.TestSuite;
import net.ion.craken.node.crud.TestReadChildren;
import net.ion.craken.node.crud.TestReadChildrenEach;
import net.ion.craken.node.crud.TestReadNodeChild;
import net.ion.craken.node.crud.TestRefChildren;
import net.ion.craken.node.crud.TestWriteChildren;
import net.ion.craken.node.crud.property.TestInnerChild;

public class TestAllChildren extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All Children") ;
		
		// beginner
		suite.addTestSuite(TestReadNodeChild.class) ;
		suite.addTestSuite(TestInnerChild.class) ;
		
		suite.addTestSuite(TestReadChildren.class) ;
		suite.addTestSuite(TestReadChildrenEach.class) ;
		suite.addTestSuite(TestWriteChildren.class) ;
		
		
		suite.addTestSuite(TestRefChildren.class) ;
	
		return suite ;
	}

}
