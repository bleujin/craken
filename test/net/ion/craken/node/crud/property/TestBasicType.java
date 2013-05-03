package net.ion.craken.node.crud.property;

import java.util.Date;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.tree.PropertyValue;
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
		assertEquals(1, found.property("int").value()) ;
		assertEquals(2L, found.property("long").value()) ;
		assertEquals(2.3f, found.property("float").value()) ;
		assertEquals(2.3d, found.property("double").value()) ;
		assertEquals("string", found.property("string").value()) ;
		assertEquals(new Date().getDate(), ((Date)found.property("date").value()).getDate()) ;
	}
	
	public void testPropertyValueAlwaysNotNull() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("nullchcek").property("boolean", true);
				return null;
			}
		}).get() ;

		assertEquals(true, session.pathBy("/nullchcek").property("null") != null) ;
		assertEquals(true, session.pathBy("/nullchcek").property("null") == PropertyValue.NotFound) ;
		assertEquals(true, session.pathBy("/nullchcek").property("null").value() == null) ;
	}
	

	
	
	
}
