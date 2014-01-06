package net.ion.craken.node.crud;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.Functions;
import net.ion.framework.db.Rows;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;

import com.google.common.base.Function;

public class TestTransformer extends TestBaseCrud {

	private ReadNode readnode;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/users/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}) ;
		this.readnode = session.pathBy("/users/bleujin");
	}
	
	public void testFirst() throws Exception {
		Integer value = readnode.transformer(new Function<ReadNode, Integer>(){
			@Override
			public Integer apply(ReadNode node) {
				return node.property("age").value(0);
			}
		});
		
		assertEquals(20, value.intValue()) ;
	}
	
	public void testToRowsFunction() throws Exception {
		Rows rows = readnode.toRows("name, age");
		
		
		assertEquals(20, rows.firstRow().getInt("age")) ;
		assertEquals("bleujin", rows.firstRow().getString("name")) ;
	}
	
	public void testJson() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).addChild("address").property("city", "seoul").parent().refTo("friend", "/hero") ;
				wsession.pathBy("/hero").property("name", "bleujin").property("age", 20).addChild("address").property("city", "seoul") ;
				return null;
			}
		}) ;
		
		JsonObject jso = session.pathBy("/bleujin").transformer(Functions.toJson()) ;
		
		Debug.line(jso.asString("children")) ;
		
		Debug.line(jso) ;
		
	}
	
	public void testJsonObject() throws Exception {
		JsonObject json = new JsonObject();
		json.put("name", "bleu") ;
		JsonObject rel = new JsonObject() ;
		rel.put("name", "jin") ;
		json.put("rel", rel) ;
		
		
		Debug.line(json.get("name"), json.get("rel")) ;
		
	}
	
	
}



