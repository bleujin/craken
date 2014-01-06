package net.ion.craken.expression;

import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllExpression extends TestCase {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All Craken Parser") ;

		suite.addTestSuite(TestColumnProjection.class) ;
		suite.addTestSuite(TestExpressionFilter.class) ;
		suite.addTestSuite(TestExpressionParser.class) ;

		suite.addTestSuite(TestParser.class) ;
		suite.addTestSuite(TestQualifiedName.class) ;
		suite.addTestSuite(TestToAdRow.class) ;
		suite.addTestSuite(TestValueObject.class) ;

		
		return suite ;
	}
	
}
