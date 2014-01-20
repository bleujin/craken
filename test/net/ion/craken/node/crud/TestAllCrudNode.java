package net.ion.craken.node.crud;

import junit.framework.TestSuite;
import net.ion.craken.io.TestException;
import net.ion.craken.node.crud.property.TestAppend;
import net.ion.craken.node.crud.property.TestBasicType;
import net.ion.craken.node.crud.property.TestInnerChild;
import net.ion.craken.node.problem.TestTransaction;

public class TestAllCrudNode extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All Node CRUD") ;

		suite.addTestSuite(TestFirst.class) ;
		suite.addTestSuite(TestWorkspaceListener.class) ;
		suite.addTestSuite(TestDefineWorkspace.class) ;
		suite.addTestSuite(TestPathBy.class) ;
		suite.addTestSuite(TestReadNode.class) ;
		suite.addTestSuite(TestExtendProperty.class) ;
		suite.addTestSuite(TestReadNodeChild.class) ;
		suite.addTestSuite(TestReadSession.class) ;
		suite.addTestSuite(TestTransaction.class) ;
		suite.addTestSuite(TestWriteNode.class) ;
		suite.addTestSuite(TestWriteSession.class) ;
		suite.addTestSuite(TestFieldIndexConfig.class) ;
		suite.addTestSuite(TestPredicate.class) ;
		suite.addTestSuite(TestCreateBy.class) ;
		suite.addTestSuite(TestRelation.class) ;
		suite.addTestSuite(TestWorkspace.class) ;
		
		suite.addTestSuite(TestBasicType.class) ;
		suite.addTestSuite(TestInnerChild.class) ;
		suite.addTestSuite(TestAppend.class) ;
		suite.addTestSuite(TestTransformer.class) ;
		
		suite.addTestSuite(TestRemoveChildren.class) ;
		suite.addTestSuite(TestException.class) ;
		return suite ;
	}
	
}
