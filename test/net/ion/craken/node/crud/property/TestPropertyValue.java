package net.ion.craken.node.crud.property;

import java.util.Date;

import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestPropertyValue extends TestCase{

	
	public void testDate() throws Exception {
		PropertyValue pv = PropertyValue.createPrimitive(new Date());
		assertEquals(true, new Date().getTime() > 1380521825847L) ;
		Debug.line(pv.value(), pv.asJsonArray().get(0).getClass()) ;

	}
}
