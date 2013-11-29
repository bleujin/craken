package net.ion.craken.node.convert.map;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;

public class TestToPropertyMap extends TestBaseCrud {

	
	public void testToMapIsUnmodifiable() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", 10) ;
				return null ;
			}
		}).get() ;

		Map<PropertyId, PropertyValue> map = session.pathBy("/bleujin").toMap();
		assertEquals("bleujin", map.get(PropertyId.normal("name")).value()) ;
		try {
			map.put(PropertyId.normal("newkey"), PropertyValue.createPrimitive("newvalue")) ;
			fail() ;
		} catch(UnsupportedOperationException expect){} ;
	}
	
	
	public void testPropertyMap() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", 10).append("age", 20) ;
				return null ;
			}
		}).get() ;

		Map<String, Object> map = session.pathBy("/bleujin").toPropertyMap(0);
		assertEquals("bleujin", map.get("name")) ;
		assertEquals(2, ((Set)map.get("age")).size()) ;
		
		try {
			map.put("newkey", "newvalue") ;
			fail() ;
		} catch(UnsupportedOperationException expect){} ;
	}
	
	
	public void testIncludeChild() throws Exception {

		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", 10).append("age", 20)
					.addChild("address").property("city", "seoul")
						.addChild("grandchild") ;
				return null ;
			}
		}).get() ;
		

		Map<String, Object> map = session.pathBy("/bleujin").toPropertyMap(1);
		Map<String, Object> addressMap = (Map<String, Object>) map.get("/address");
		assertEquals(1, addressMap.size()) ;

		Map<String, Object> gchildMap = (Map<String, Object>) addressMap.get("/grandchild");
		assertEquals(true, gchildMap == null); // depth == 1
	}

	public void testMultipleChild() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", 10).append("age", 20)
					.addChild("address").property("city", "seoul").parent()
					.addChild("pic").property("name", "bleujin.jpg") ;
				return null ;
			}
		}).get() ;
		
		Map<String, Object> map = session.pathBy("/bleujin").toPropertyMap(1);
		Map<String, Object> addressMap = (Map<String, Object>) map.get("/address");
		assertEquals(1, addressMap.size()) ;

		Map<String, Object> picMap = (Map<String, Object>) map.get("/pic");
		assertEquals(1, picMap.size()) ;
		assertEquals("bleujin.jpg", picMap.get("name")) ;
	}
	
	
	public void testReference() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", 10).append("age", 20)
					.refTos("friends", "/hero")
					.refTos("friends", "/jin")
					.refTos("friends", "/notfound");
				
				wsession.root().addChild("/hero").property("name", "hero") ;
				wsession.root().addChild("/jin").property("name", "jin") ;
				
				return null ;
			}
		}).get() ;
		
		Map<String, Object> map = session.pathBy("/bleujin").toPropertyMap(1);
		Set<Map<String, Object>> refs = (Set<Map<String, Object>>) map.get("@friends");
		
		assertEquals(3, refs.size()) ;
		final Iterator<Map<String, Object>> iter = refs.iterator();
		assertEquals("hero", iter.next().get("name")) ;
		assertEquals("jin", iter.next().get("name")) ;
		assertEquals(true, iter.next().get("name") == null) ;
	}
	
	
}
