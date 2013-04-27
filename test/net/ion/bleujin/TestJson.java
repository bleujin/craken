package net.ion.bleujin;

import net.ion.framework.parse.gson.JsonNull;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestJson extends TestCase {

	
	public void testArray() throws Exception {
		JsonObject jso = JsonObject.fromString("{name:'bleujin', age:20000000000}") ;
		
		Debug.line(jso.asInt("age"), jso.asLong("age"), jso.asBigDecimal("age").getClass(), jso.asString("age").getClass()) ;
	}
	
	public void testJsonNull() throws Exception {
		Debug.line(JsonNull.INSTANCE) ;
	}
}
