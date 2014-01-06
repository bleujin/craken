package net.ion.craken.node.tlog;

import java.io.InputStream;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.tree.Fqn;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import com.amazonaws.util.StringInputStream;

public class TestSaveLog extends TestBaseCrud {

	
	public void testRun() throws Exception {
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.tranId("12345") ;
				
				wsession.createBy("/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/bleujin").property("name", "hero").property("age", 20).blob("blob", new StringInputStream("Long Long String"))  ;
				return null;
			}
		}) ;
		
		
		final ReadNode readNode = session.pathBy("/bleujin");
		assertEquals("hero", readNode.property("name").stringValue()) ;
		
		
		readNode.toRows("name, age, blob").debugPrint() ;
		
		InputStream input = readNode.property("blob").asBlob().toInputStream();
		Debug.line(IOUtil.toStringWithClose(input)) ;
		
		
		session.pathBy(Fqn.TRANSACTIONS.toString() + "/12345").toRows("time, config").debugPrint() ;
		
		ReadNode logNode = session.ghostBy(Fqn.TRANSACTIONS.toString() + "/12345");
		InputStream tranInput = logNode.property("tran").asBlob().toInputStream();
		
		
		JsonObject json = JsonObject.fromString(IOUtil.toStringWithClose(tranInput));
		
		assertEquals(true, json.asLong("time") > 0 ) ;
		JsonArray logs = json.asJsonArray("logs");
		for (JsonElement log : logs) {
			assertEquals("/bleujin", log.getAsJsonObject().asString("path")) ;
		}
		
//		Debug.line(IOUtil.toStringWithClose(tranInput)) ;
		
	}
	
	
	
}
