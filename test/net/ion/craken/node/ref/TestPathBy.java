package net.ion.craken.node.ref;

import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestPathBy extends TestBaseCrud {

	
	public void testNotFoundPath() throws Exception {
		
		assertEquals(true, ! session.exists("/bleujin")) ;
		assertEquals(true, session.pathBy("/bleujin") != null) ; // not null 
		
		assertEquals(0, session.pathBy("/bleujin").keys().size()) ;
	}
}
