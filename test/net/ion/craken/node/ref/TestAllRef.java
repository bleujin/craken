package net.ion.craken.node.ref;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.ion.craken.node.crud.TestRelation;

public class TestAllRef extends TestCase{

	
	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All Reference");
		
		suite.addTestSuite(TestRelation.class) ;
		suite.addTestSuite(TestRefNode.class) ;
		suite.addTestSuite(TestRefPathBy.class) ;
		return suite ;
	}
}
