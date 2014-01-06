package net.ion.craken.loaders.lucene.tran;

import java.util.Set;

import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.parse.gson.JsonObject;

public class TestSessionConfig extends TestBaseCrud{

	
	public void testInsert() throws Exception {
		String tranId1 = session.tranSync(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				wsession.fieldIndexConfig().num("age") ;
				
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.pathBy("/hero").property("name", "hero").property("age", 20) ;
				return wsession.tranId();
			}
		}) ;
		String tranId2 = session.tranSync(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				wsession.fieldIndexConfig().num("age") ;
				
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				return wsession.tranId();
			}
		}) ;
		
		session.pathBy("/__transactions").children().toAdRows("config, time").debugPrint() ;
		
		assertEquals(tranId1, session.pathBy("/hero").ref("__transaction").fqn().toString()) ;
		assertEquals(tranId2, session.pathBy("/bleujin").ref("__transaction").fqn().toString()) ;
	}
	
	public void testTranLog() throws Exception {
		String tranId1 = session.tranSync(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				wsession.fieldIndexConfig().num("age").keyword("name").ignoreBodyField() ;
				
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.pathBy("/hero").property("name", "hero").property("age", 20) ;
				wsession.pathBy("/hero").removeSelf() ;
				wsession.pathBy("/hero").removeChildren() ;
				return wsession.tranId();
			}
		}) ;
		
		final ReadNode tranNode = session.pathBy(tranId1);
		Set<String> logs = tranNode.property("tlogs").asSet() ;
		assertEquals("[MODIFY/bleujin, MODIFY/hero, REMOVE/hero, REMOVECHILDREN/hero]", logs.toString()) ;
		IndexWriteConfig wconfig = JsonObject.fromString(tranNode.property("config").stringValue()).getAsObject(IndexWriteConfig.class);
		assertEquals(true, wconfig.isIgnoreBodyField()) ;
		assertEquals(FieldIndex.NUMBER, wconfig.fieldIndex("age")) ;
		assertEquals(FieldIndex.KEYWORD, wconfig.fieldIndex("name")) ;
		assertEquals(FieldIndex.UNKNOWN, wconfig.fieldIndex("dd")) ;
	}
	
	public void testTranSearch() throws Exception {
		String currTime = "" + System.currentTimeMillis() ;
		
		String tranId1 = session.tranSync(new TransactionJob<String>() {
			@Override
			public String handle(WriteSession wsession) throws Exception {
				wsession.fieldIndexConfig().num("age").keyword("name").ignoreBodyField() ;
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.pathBy("/hero").property("name", "hero").property("age", 20) ;
				return wsession.tranId();
			}
		}) ;
		
		assertEquals(1, session.queryRequest("").gte("time", currTime).find().totalCount()) ;
	}
	
	
	
	
	
	
	
}
