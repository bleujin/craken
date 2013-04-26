package net.ion.craken.node.crud.property;

import java.util.Date;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestBasicType extends TestBaseCrud {

	
	public void testType() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("type").property("boolean", true).property("int", 1).property("long", 2L).property("float", 2.3f).property("double", 2.3d).property("string", "string").property("date", new Date()) ;
				return null;
			}
		}).get() ;
		
		ReadNode found = session.pathBy("/type");
		assertEquals(1, found.property("int")) ;
		assertEquals(2L, found.property("long")) ;
		assertEquals(2.3f, found.property("float")) ;
		assertEquals(2.3d, found.property("double")) ;
		assertEquals("string", found.property("string")) ;
		assertEquals(new Date().getDate(), found.property("date").asDate()) ;
	}
	
//	public void testInner() throws Exception {
//		session.tran(new TransactionJob<Void>() {
//			@Override
//			public Void handle(WriteSession wsession) {
//				wsession.root().addChild("/bleujin").inner("address").property("city", "busan") ;
//				return null;
//			}
//		}).get() ;
//		
//		Debug.line(session.pathBy("/bleujin").inner("address")) ;
//
//		
//	}
	
	
	
}
