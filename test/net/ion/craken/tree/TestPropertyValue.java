package net.ion.craken.tree;

import java.util.Date;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.framework.util.Debug;

public class TestPropertyValue extends TestBaseCrud{

	
	public void testDate() throws Exception {
		PropertyValue pv = PropertyValue.createPrimitive(new Date());
		assertEquals(true, new Date().getTime() > 1380521825847L) ;
		Debug.line(pv.value(), pv.asJsonArray().get(0).getClass()) ;

	}
	
	public void testNotAllowDouble() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("double", 2000.0d) ;
				return null;
			}
		}) ;
		
		final PropertyValue property = session.pathBy("/bleujin").property("double");
		assertEquals(2000, property.intValue(0));
	}
	
	
	public void testUnSet() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").append("friends", "hero", "jin", "") ;
				return null;
			}
		}) ;

		assertEquals(3, session.pathBy("/bleujin").property("friends").asSet().size());

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").unset("friends") ;
				return null;
			}
		}) ;

		assertEquals(0, session.pathBy("/bleujin").property("friends").asSet().size());
	}
	
	
	public void testUnSetIn() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").append("friends", "hero", "jin", "bleu") ;
				return null;
			}
		}) ;
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").unset("friends", "hero", "bleu");
				return null;
			}
		}) ;
		
		assertEquals(1, session.pathBy("/bleujin").property("friends").asSet().size());
		assertEquals("jin", session.pathBy("/bleujin").property("friends").stringValue());
	}
	
	
	
	public void testUnRef() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").refTos("friends", "/bleujin", "/hero", "/jin").property("name", "bleujin") ;
				wsession.pathBy("/hero").property("name", "hero")  ;
				wsession.pathBy("/jin");
				return null;
			}
		}) ;
		assertEquals(3, session.pathBy("/bleujin").refs("friends").toList().size()) ;

		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").unRefTos("friends", "/bleujin", "/jin") ;
				return null;
			}
		}) ;
		
		assertEquals(1, session.pathBy("/bleujin").refs("friends").toList().size());
		assertEquals("hero", session.pathBy("/bleujin").ref("friends").property("name").stringValue());
	}
	
	
	public void testDefaultValueWhenNotExist() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").stringValue()) ;
		
		assertEquals(20, session.pathBy("/bleujin").property("age").intValue(20)) ;
		assertEquals(20, session.ghostBy("/bleujin").property("age").intValue(20)) ;
		assertEquals("not", session.pathBy("/bleujin").property("notfound").defaultValue("not")) ;
		assertEquals(new Integer(2), session.pathBy("/bleujin").property("notfound").defaultValue(2)) ;
		
		
		// when not exist
		assertEquals(20, session.ghostBy("/hero").property("age").intValue(20)) ;
		assertEquals("20", session.ghostBy("/hero").property("age").defaultValue("20")) ;
		assertEquals(new Integer(20), session.ghostBy("/hero").property("age").defaultValue(20)) ;
	}
	
	
	public void testRef() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").refTo("self", "/bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals("/bleujin", session.pathBy("/bleujin").propertyId(PropertyId.refer("self")).asString()) ;
	}
	

	
}
