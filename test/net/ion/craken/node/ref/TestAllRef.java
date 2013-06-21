package net.ion.craken.node.ref;

import net.ion.craken.node.convert.from.TestFromJson;
import net.ion.craken.node.convert.map.TestToPropertyMap;
import net.ion.craken.node.convert.rows.TestToRows;
import net.ion.craken.node.convert.to.TestChild;
import net.ion.craken.node.convert.to.TestToChildBean;
import net.ion.craken.node.convert.to.TestToFlatBean;
import net.ion.craken.node.convert.to.TestToRefBean;
import net.ion.craken.node.convert.to.type.TestPrimitiveProperty;
import net.ion.craken.node.convert.to.type.TestReference;
import net.ion.craken.node.convert.to.type.TestValueObjectProperty;
import net.ion.framework.db.servant.TestChannelServant;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class TestAllRef extends TestCase{

	
	public static TestSuite suite(){
		TestSuite suite = new TestSuite();
		
		suite.addTestSuite(TestRefNode.class) ;
		suite.addTestSuite(TestRefPathBy.class) ;
		return suite ;
	}
}
