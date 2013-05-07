package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.framework.util.Debug;

public class TestExtendProperty extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/users/bleujin").property("name", "jin").addChild("address").property("city", "seoul") ;
				wsession.pathBy("/boards/freeboard/1").property("subject", "hello").refTo("writer", "/users/bleujin").parent().property("name", "free") ;
				return null;
			}
		}).get() ;
	}
	
	public void testNormal() throws Exception {
		assertEquals("hello", session.pathBy("boards/freeboard/1").extendProperty("subject").value()) ;
	}
	
	public void testChild() throws Exception {
		assertEquals("hello", session.pathBy("boards/freeboard").extendProperty("1/subject").value()) ;
		assertEquals(true, session.pathBy("boards/freeboard").extendProperty("2/subject").value() == null) ;
	}
	
	public void testParent() throws Exception {
		assertEquals("free", session.pathBy("/boards/freeboard").property("name").value()) ;
		assertEquals("free", session.pathBy("boards/freeboard/1").extendProperty("../name").value()) ;
		
	}
	
	public void testRef() throws Exception {
		assertEquals("jin", session.pathBy("boards/freeboard/1").extendProperty("writer@name").value()) ;
	}

	public void testComposite() throws Exception {
		assertEquals("jin", session.pathBy("boards/freeboard/1").extendProperty("../1/writer@name").value()) ;
	}
	
}
