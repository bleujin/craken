package net.ion.craken.node.convert;

import junit.framework.TestCase;
import junit.framework.TestSuite;
import net.ion.craken.node.convert.from.TestFromJson;
import net.ion.craken.node.convert.map.TestToPropertyMap;
import net.ion.craken.node.convert.rows.TestToRows;
import net.ion.craken.node.convert.rows.TestToRowsSort;
import net.ion.craken.node.convert.to.TestChild;
import net.ion.craken.node.convert.to.TestToChildBean;
import net.ion.craken.node.convert.to.TestToFlatBean;
import net.ion.craken.node.convert.to.TestToRefBean;
import net.ion.craken.node.convert.to.type.TestPrimitiveProperty;
import net.ion.craken.node.convert.to.type.TestReference;
import net.ion.craken.node.convert.to.type.TestValueObjectProperty;

public class TestAllConvert extends TestCase{

	
	public static TestSuite suite(){
		TestSuite suite = new TestSuite("Test All Node Convert");
		
		suite.addTestSuite(TestFromJson.class) ;
		suite.addTestSuite(TestToPropertyMap.class) ;
		suite.addTestSuite(TestToRows.class) ;
		suite.addTestSuite(TestToRowsSort.class) ;
		// to
		suite.addTestSuite(TestPrimitiveProperty.class) ;
		suite.addTestSuite(TestReference.class) ;
		suite.addTestSuite(TestValueObjectProperty.class) ;
		
		suite.addTestSuite(TestChild.class) ;
		suite.addTestSuite(TestToChildBean.class) ;
		suite.addTestSuite(TestToFlatBean.class) ;
		suite.addTestSuite(TestToRefBean.class) ;
		
		
		return suite ;
	}
}
