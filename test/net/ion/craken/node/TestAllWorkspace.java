package net.ion.craken.node;

import net.ion.craken.io.TestExceptionHandle;
import net.ion.craken.node.crud.TestCDDHandler;
import net.ion.craken.node.crud.TestDefineWorkspace;
import net.ion.craken.node.crud.TestOtherWorkspace;
import net.ion.craken.node.crud.TestWorkspaceConfig;
import net.ion.craken.node.crud.TestWorkspaceListener;
import net.ion.craken.node.problem.TestTransaction;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TestAllWorkspace {

	public static Test suite() {
		TestSuite suite = new TestSuite("workspace") ;
		
		suite.addTestSuite(TestDefineWorkspace.class) ;
		suite.addTestSuite(TestWorkspaceConfig.class) ;
		suite.addTestSuite(TestOtherWorkspace.class);
		suite.addTestSuite(TestTransaction.class) ;
		suite.addTestSuite(TestSync.class) ;
		suite.addTestSuite(TestWorkspaceListener.class) ;
		suite.addTestSuite(TestExceptionHandle.class) ;

		suite.addTestSuite(TestCDDHandler.class);
		
		return suite;
	}

}
