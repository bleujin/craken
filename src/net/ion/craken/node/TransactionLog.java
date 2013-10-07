package net.ion.craken.node;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import net.ion.craken.loaders.lucene.DocEntry;
import net.ion.craken.node.IndexWriteConfig.FieldIndex;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Action;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.search.SearchRequest;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.TermQuery;
import org.infinispan.atomic.AtomicMap;

public class TransactionLog{

	private String id ;
	private String path;
	private Touch touch;
	private JsonObject val;
	private Action action = Action.MERGE ;
	private String parentPath ;

	public static class PropId {
		public final static PropertyId ADDRESS = PropertyId.normal("address") ;
		public final static PropertyId CONFIG = PropertyId.normal("config") ;
		public final static PropertyId TIME = PropertyId.normal("time") ;

		public final static PropertyId ID = PropertyId.normal("id") ;
		public final static PropertyId PATH = PropertyId.normal("path") ;
		public final static PropertyId TOUCH = PropertyId.normal("touch") ;
		public final static PropertyId VAL = PropertyId.normal("val") ;
	}
	
	private TransactionLog(String id, String path, Touch touch, JsonObject val){
		this.id = id ;
		this.path = path ;
		this.touch = touch ;
		this.val = val ;
		this.parentPath = path.equals("/") ? ""  : StringUtil.defaultIfEmpty(StringUtil.substringBeforeLast(path, "/"), "/") ;
	}

	public static TransactionLog create(String id, String path, Touch touch, JsonObject val) {
		return new TransactionLog(id, path, touch, val);
	}

	public static boolean isLogKey(TreeNodeKey key){
		return key.fqnString().startsWith("/__transactions/") ;
	}
	
	public static String newTranId(String idString){
		return "/__transactions/" + idString ;
	}
	
	
	public Touch touchType() {
		return touch;
	}
	
	Action action(){
		return action ;
	}
	
	JsonObject props(){
		return val.asJsonObject("properties") ;
	}
	
	JsonObject rels(){
		return val.asJsonObject("references") ;
	}
	
	public String path(){
		return path ;
	}
	
	String parentPath(){
		return this.parentPath ;
	}
	
	String id(){
		return id ;
	}
	
	
	@Override
	public boolean equals(Object obj){
		TransactionLog that = (TransactionLog) obj ;
		return this.id.equals(that.id) ;
	}
	
	@Override
	public int hashCode(){
		return id.hashCode() ;
	}
	
	public String toString(){
		StringBuilder result = new StringBuilder();
		result.append("path:" + path).append(", action:" + action).append(", props:" + props()) ;
		
		return result.toString();
	}
	
	public void writeDocument(IndexSession isession, IndexWriteConfig config) throws IOException{
		final WriteDocument propDoc = isession.newDocument(path());
		JsonObject jobj = new JsonObject();
		jobj.addProperty(DocEntry.ID, path());
		// jobj.addProperty(DocEntry.LASTMODIFIED, System.currentTimeMillis());
		jobj.add(DocEntry.PROPS, fromMapToJson(propDoc, config, this));

		propDoc.add(MyField.manual(DocEntry.VALUE, jobj.toString(), org.apache.lucene.document.Field.Store.YES, Index.NOT_ANALYZED));
		
		if (action() == Action.CREATE) 
			isession.insertDocument(propDoc) ; 
		else isession.updateDocument(propDoc);

	}

	
	private JsonObject fromMapToJson(WriteDocument doc, IndexWriteConfig iwconfig, TransactionLog log) {
		JsonObject jso = new JsonObject();
		String parentPath = log.parentPath();
		doc.keyword(DocEntry.PARENT, parentPath);
		doc.number(DocEntry.LASTMODIFIED, System.currentTimeMillis());
		
		for (Entry<String, JsonElement> entry : log.props().entrySet()) {
			final String propId = entry.getKey();
			JsonArray pvalue = entry.getValue().getAsJsonArray();
			jso.add(propId, entry.getValue().getAsJsonArray()); 
			for (JsonElement e : pvalue.toArray()) {
				if (e == null) continue ;
				FieldIndex fieldIndex = iwconfig.fieldIndex(propId) ; 
				fieldIndex.index(doc, propId, e.isJsonObject() ? e.toString() : e.getAsString()) ;
			}
		}
		for (Entry<String, JsonElement> entry : log.rels().entrySet()) {
			final String propId = entry.getKey();
			JsonArray pvalue = entry.getValue().getAsJsonArray();
			jso.add(propId, entry.getValue().getAsJsonArray()); // if type == refer, @
			for (JsonElement e : pvalue.toArray()) {
				if (e == null) continue ;
				FieldIndex.KEYWORD.index(doc, '@' + propId, e.getAsString()) ;
			}
		}
		
		return jso;
	}

	
}