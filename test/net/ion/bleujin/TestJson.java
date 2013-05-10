package net.ion.bleujin;

import java.io.StringReader;

import net.ion.framework.parse.gson.JsonNull;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.stream.JsonReader;
import net.ion.framework.parse.gson.stream.JsonToken;
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
	
	
	public void testJsonReader() throws Exception {
		JsonReader jreader = new JsonReader(new StringReader("{person:\"bleujin\", age:20, address:{city:\"seoul\"}}"));
		jreader.setLenient(true) ;
		
		jreader.beginObject() ;
		while(jreader.hasNext()){
			JsonToken token = jreader.peek();
			if (token == JsonToken.NAME){
				jreader.nextName() ;
			} else if (token == JsonToken.STRING){
				jreader.nextString() ;
			} else if (token == JsonToken.NUMBER){
				jreader.nextLong() ;
			} else if (token == JsonToken.BEGIN_OBJECT){
				jreader.beginObject() ;
			} else if (token == JsonToken.END_OBJECT){
				jreader.endObject() ;
			}
		}
		
		
		jreader.endObject() ;
	}
}
