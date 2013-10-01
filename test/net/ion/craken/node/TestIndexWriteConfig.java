package net.ion.craken.node;

import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestIndexWriteConfig extends TestCase {

	public void testConfig() throws Exception {
		IndexWriteConfig wconfig = new IndexWriteConfig();
		wconfig.num("age", "bun").keyword("name").ignoreBodyField() ;
		
		JsonObject json = wconfig.toJson() ;
		
		Debug.line(json) ;
		IndexWriteConfig load = json.getAsObject(IndexWriteConfig.class);
		
		assertEquals(true, load.isIgnoreBodyField()) ;
		assertEquals(IndexWriteConfig.FieldIndex.NUMBER, load.fieldIndex("age")) ;
		assertEquals(IndexWriteConfig.FieldIndex.NUMBER, load.fieldIndex("bun")) ;
		assertEquals(IndexWriteConfig.FieldIndex.KEYWORD, load.fieldIndex("name")) ;
		assertEquals(IndexWriteConfig.FieldIndex.UNKNOWN, load.fieldIndex("greet")) ;
	}
}
