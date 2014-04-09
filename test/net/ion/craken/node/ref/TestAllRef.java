package net.ion.craken.node.ref;

import net.ion.craken.node.crud.TestRelation;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllRef extends TestCase{

	
	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All Reference");
		
		suite.addTestSuite(TestRelation.class) ;
		suite.addTestSuite(TestRefNode.class) ;
		suite.addTestSuite(TestRefPathBy.class) ;
		return suite ;
	}
}
