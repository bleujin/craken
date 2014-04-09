package net.ion.craken.node.script;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.script.JsonBuilder;
import net.ion.framework.db.Rows;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestJsonBuilder extends TestCase {

	
	public void testSimple() throws Exception {
		JsonElement json = JsonBuilder.instance().newInner().property("name", "bleujin").buildJson() ;
		assertEquals(true, json.isJsonObject());
		assertEquals("bleujin", json.getAsJsonObject().asString("name"));
	}
	
	
	public void testJsonArray() throws Exception {
		JsonElement json = JsonBuilder.instance().newInlist().property("name", "bleujin").property("age", 20).next().property("name", "hero").property("age", 30).buildJson() ;
		Debug.line(json);
	}
	
	public void testWithCraken() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest() ;
		ReadSession session = r.start().login("test") ;
		session.tranSync(TransactionJobs.dummy("/bleujin", 10)) ;
		
		Rows rows = JsonBuilder.instance().newInlist(session.pathBy("/bleujin").children().ascending("dummy"), "dummy, name").buildRows() ;
		rows.debugPrint(); 
	}
	
	
	
	
	
	
}
