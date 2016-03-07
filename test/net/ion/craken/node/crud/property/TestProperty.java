package net.ion.craken.node.crud.property;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.infinispan.configuration.cache.CacheMode;

import net.ion.craken.node.IndexWriteConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectId;
import net.ion.framework.util.SetUtil;

public class TestProperty extends TestBaseCrud {

	
	public void testIdIsHangul() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("이름", "bleu").property("성", "jin").property("풀네임", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals("jin", session.pathBy("/bleujin").property("성").value()) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("풀네임").value()) ;
	}
	
	public void testCaseSensitive() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("FirstName", "bleu").property("LastName", "jin").property("FullName", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals("jin", session.pathBy("/bleujin").property("LastName").value()) ;
		assertEquals(PropertyValue.NotFound, session.pathBy("/bleujin").property("lastname") ) ;
		
	}
	
	public void testIdSlash() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("/name/first", "1").property("/name/last", "3").append("/name/first", "2");
				return null;
			}
		}) ;
		
		final ReadNode found = session.pathBy("/bleujin");
		assertEquals("1", found.property("/name/first").value()) ;
		Set<String> set = found.property("/name/first").asSet() ;
		assertEquals("1", set.toArray(new String[0])[0]) ;
		
		for (PropertyId pid : found.normalKeys()) {
			Debug.line(pid) ;
		}
		JsonObject jsonExpr = found.transformer(Functions.<ReadNode>toJsonExpression()) ;
		JsonObject readJson = JsonObject.fromString(jsonExpr.toString()) ;
		Debug.line(readJson) ;
	}
	
	
	
	public void testReadJson() throws Exception {
		String jsonExpr = " {\"properties\":{\"/name/last\":[\"3\"],\"/name/first\":[\"1\",\"2\"]},\"references\":{\"__transaction\":[\"/__transactions/52315bc4198e2b58945d759c\"]}}" ;
		Debug.line(JsonObject.fromString(jsonExpr)) ;
	}
	
	
	public void testDashJson() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/tran")
					.property("time", 3L)
					.property("config", IndexWriteConfig.Default.toJson().toString())
					.child("1231")
						.property("id", "1231").property("path", "/bleujin").property("touch", Touch.MODIFY.name()).property("val", "{\"properties\":{\"/name/last\":[\"3\"],\"/name/first\":[\"1\",\"2\"]},\"references\":{\"__transaction\":[\"/__transactions/52315bc4198e2b58945d759c\"]}}").parent()
					.child("1232")
						.property("id", "1232").property("path", "/bleujin/hero").property("touch", Touch.MODIFY.name()).property("val", "{\"properties\":{\"/name/last\":[\"3\"],\"/name/first\":[\"1\",\"2\"]},\"references\":{\"__transaction\":[\"/__transactions/52315bc4198e2b58945d759c\"]}}").parent()
					.child("1233")
						.property("id", "1233").property("path", "/bleujin/hero").property("touch", Touch.REMOVECHILDREN.name()).parent()
					.child("1234")
						.property("id", "1234").property("path", "/bleujin/hero").property("touch", Touch.REMOVE.name()).parent()
					;
				return null;
			}
		}) ;
		
		final ReadNode tranNode = session.pathBy("/tran");
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {

				wsession.iwconfig(JsonObject.fromString(tranNode.property("config").stringValue()).getAsObject(IndexWriteConfig.class)) ;
				
				for (ReadNode found : tranNode.children().ascending("id")) {
					if (Touch.MODIFY.toString().equals(found.property("touch").stringValue())){
						String path = found.property("path").stringValue() ;
						JsonObject jvals = JsonObject.fromString(found.property("val").stringValue()) ;
						
						Map<String, Object> props = jvals.asJsonObject("properties").toMap();
						final WriteNode targetNode = wsession.pathBy(path);
						for (Entry<String, Object> entry : props.entrySet()) {
							targetNode.property(entry.getKey(), entry.getValue()) ;
						}
						
						JsonObject rels = jvals.asJsonObject("references");
						for (Entry<String, JsonElement> entry : rels.entrySet()) {
							for (JsonElement toPath : entry.getValue().getAsJsonArray().toArray()) {
								targetNode.refTo(entry.getKey(), toPath.toString()) ;
							}
						} 
					} else if (Touch.REMOVE.toString().equals(found.property("touch").stringValue())){
						String path = found.property("path").stringValue() ;
						wsession.pathBy(path).removeSelf() ;
					} else if (Touch.REMOVECHILDREN.toString().equals(found.property("touch").stringValue())){
						String path = found.property("path").stringValue() ;
						wsession.pathBy(path).removeChildren() ;
					}
				}
				
				return null;
			}
		}) ;
		
		session.pathBy("/").children().debugPrint() ; 
		assertEquals(1, session.pathBy("/bleujin").property("/name/first").intValue(0)) ;
		assertEquals(2, session.pathBy("/bleujin").property("/name/first").asSet().size()) ;
		
		assertEquals(true, ! session.exists("/bleujin/hero")) ;
	}
	
	
	public void testNullValue() throws Exception {
		super.r.createWorkspace("local", WorkspaceConfigBuilder.gridDir("./resource/temp").distMode(CacheMode.LOCAL)) ;
		
		ReadSession msession = r.login("local");
		
		msession.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/dev/bleujin").property("name", null) ;
				return null;
			}
			
		}) ;
		
		msession.root().walkChildren().debugPrint(); 
	}	
	
	
	public void xtestObjectId() throws Exception {
		Set<String> oids = SetUtil.newSet() ;
		for (int i : ListUtil.rangeNum(10000)) {
			oids.add(new ObjectId().toString()) ;
		}
		
		assertEquals(10000, oids.size()) ;
	}
	
	
	
	public void testExpertUse() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleu").property("age", 20).property("birth", new Date());
				return null;
			}
		}) ;
		
		assertEquals("bleu", session.pathBy("/bleujin").asString("name")) ;
		assertEquals(true, session.pathBy("/bleujin").defaultValue("age", 20) == 20) ;
	}
	
	
	
}
