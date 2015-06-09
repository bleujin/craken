package net.ion.craken;

import java.io.File;
import java.io.IOException;

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
import net.ion.framework.util.FileUtil;

public class TestAllCraken extends TestSuite {

	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All CrakenNode") ;
		try {
			FileUtil.deleteDirectory(new File("./resource/store"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// beginner
		suite.addTest(TestAllCrudNode.suite()) ;
		suite.addTest(TestAllRef.suite()) ;
		suite.addTest(TestAllChildren.suite()) ;

		// intermediate
		suite.addTest(TestAllSearchNode.suite()) ;
		suite.addTest(TestAllConvert.suite()) ;

		
		// advanced
		suite.addTest(TestAllWorkspace.suite()) ;
		suite.addTest(TestAllMapReduce.suite()) ;
		
		
		// etc
		suite.addTestSuite(TestIndexWriteConfig.class);
		suite.addTestSuite(TestSessionTracer.class);
		return suite ;
	}
	
}
