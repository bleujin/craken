package net.ion.craken.loaders;

import net.ion.craken.loaders.lucene.TestISearchPropertyTypeSave;
import net.ion.craken.loaders.neo.TestNeoPropertyTypeSave;
import net.ion.craken.loaders.rdb.TestRDBPropertyTypeSave;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllLoaders extends TestCase {

	
	public static TestSuite suite(){
		TestSuite result = new TestSuite() ;
		
		result.addTestSuite(TestISearchPropertyTypeSave.class);
		result.addTestSuite(TestNeoPropertyTypeSave.class);
		result.addTestSuite(TestRDBPropertyTypeSave.class);
		
		return result ;
	}
}
