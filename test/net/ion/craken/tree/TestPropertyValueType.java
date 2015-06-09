package net.ion.craken.tree;

import junit.framework.TestCase;
import net.ion.craken.node.crud.tree.NodeNotValidException;
import net.ion.craken.node.crud.tree.impl.GridBlob;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.PropertyValue.VType;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;

public class TestPropertyValueType extends TestCase {

	
	public void testViewType() throws Exception {
		assertEquals(VType.BOOL, PropertyValue.createPrimitive(true).type()) ;
		assertEquals(VType.INT, PropertyValue.createPrimitive(3).type()) ;
		assertEquals(VType.LONG, PropertyValue.createPrimitive(0L).type()) ;
		assertEquals(VType.DOUB, PropertyValue.createPrimitive(3.0D).type()) ;
		assertEquals(VType.STR, PropertyValue.createPrimitive("HELLO").type()) ;
		assertEquals(VType.BLOB, PropertyValue.createPrimitive(GridBlob.create(null, "/path")).type()) ;
		assertEquals(VType.UNKNOWN, PropertyValue.createPrimitive(null).type()) ;
	}
	
	
	public void testSameAppend() throws Exception {
		PropertyValue pv = PropertyValue.createPrimitive(true)  ;
		pv.append(false) ;
		assertEquals(2, pv.asSet().size()) ;
		
		assertEquals(Boolean.TRUE, pv.value()) ;
		assertEquals(Boolean.FALSE, pv.asSet().toArray()[1]) ;
	}
	
	public void testSameType() throws Exception {
		PropertyValue pv = PropertyValue.createPrimitive("Hello")  ;
		try {
			pv.append(false) ;
			fail();
		} catch(NodeNotValidException expect){
		}
	}
	
	
	public void testAfterBlank() throws Exception {
		PropertyValue pv = PropertyValue.createBlank() ;
		pv.append(true) ;
		
		assertEquals(1, pv.asSet().size()) ;
		pv.append(false) ;
		assertEquals(2, pv.asSet().size()) ;
		
		try {
			pv.append("") ;
			fail();
		} catch(NodeNotValidException expect){
		}
	}

	public void testReplaceValue() throws Exception {
		PropertyValue pv = PropertyValue.createPrimitive(new PropertyValue.ReplaceValue() {
			@Override
			public String replaceValue() {
				return "hello";
			}
			public VType vtype(){
				return VType.UNKNOWN ;
			}
		});
		
		assertEquals("hello", pv.stringValue()) ;
		assertEquals(VType.UNKNOWN, pv.type()) ;
	}

	public void testBlob() throws Exception {
		PropertyValue pv = PropertyValue.createPrimitive(GridBlob.create(null, "/path"))  ;
		assertEquals(VType.BLOB, pv.type());
		assertEquals("/path", GridBlob.class.getMethod("path").invoke(pv.value()));
	}
	
	
	
	public void testJson() throws Exception {
		PropertyValue pv = PropertyValue.createPrimitive("Yahooo")  ;
		JsonObject json = pv.json() ;
		
		assertEquals("STR", json.asString("vtype")) ;
		assertEquals("Yahooo", json.asJsonArray("vals").iterator().next().getAsString()) ;
		Debug.line(json.toString());
	}
	
	
	
}
