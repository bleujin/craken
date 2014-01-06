package net.ion.craken.node;

import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import java.util.Map.Entry;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.loaders.lucene.DocEntry;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.stream.JsonWriter;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.MyField;
import net.ion.nsearcher.common.WriteDocument;
import net.ion.nsearcher.index.IndexSession;

import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;

public class IndexWriteConfig implements Serializable{

	private static final long serialVersionUID = 2475651350981574004L;
	public enum FieldIndex {
		UNKNOWN {
			@Override
			public void index(WriteDocument doc, String propId, String value) {
				doc.unknown(propId, value) ;
			}
		}, IGNORE {
			@Override
			public void index(WriteDocument doc, String propId, String value) {
				return ;
			}
		}, KEYWORD {
			@Override
			public void index(WriteDocument doc, String propId, String value) {
				doc.keyword(propId, value) ;
			}
		}, NUMBER {
			@Override
			public void index(WriteDocument doc, String propId, String value) {
				doc.number(propId, Long.parseLong(value)) ;
			}
		}, DATETIME {
			@Override
			public void index(WriteDocument doc, String propId, String value) {
				doc.date(propId, NumberUtil.toInt(StringUtil.substringBefore(value, "-")), NumberUtil.toInt(StringUtil.substringAfter(value, "-"))) ;
			}
		}, TEXT {
			@Override
			public void index(WriteDocument doc, String propId, String value) {
				doc.text(propId, value) ;
			}
		};
		
		public abstract void index(WriteDocument doc, String propId, String value) ;
	}
	
	private Map<String, FieldIndex> fieldIndexes = MapUtil.newMap() ;
	private boolean ignoreBody;  
	private boolean ignoreIndex ;
	
	public final static IndexWriteConfig Default = new IndexWriteConfig() ;
	
	
	public IndexWriteConfig ignore(String... fields){
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.IGNORE) ;
		}
		
		return this ;
	}

	public IndexWriteConfig ignoreIndex() {
		this.ignoreIndex = true ;
		return this ;
	}
	
	

	
	public IndexWriteConfig keyword(String... fields) {
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.KEYWORD) ;
		}
		return this ;
	}

	public IndexWriteConfig text(String... fields) {
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.TEXT) ;
		}
		return this ;
	}

	public IndexWriteConfig num(String... fields) {
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.NUMBER) ;
		}
		return this ;
	}

	public IndexWriteConfig date(String... fields) {
		for (String field : fields) {
			fieldIndexes.put(field, FieldIndex.DATETIME) ;
		}
		return this ;
	}

	public FieldIndex fieldIndex(String propId) {
		if (ignoreIndex) return FieldIndex.IGNORE ;
		return ObjectUtil.coalesce(fieldIndexes.get(propId), FieldIndex.UNKNOWN);
	}

	public IndexWriteConfig ignoreBodyField(){
		this.ignoreBody = true ;
		return this ;
	}

	public boolean isIgnoreBodyField(){
		return ignoreBody ;
	}

	public JsonObject toJson() {
		return JsonParser.fromObject(this).getAsJsonObject() ;
	}

	public void writeJson(JsonWriter jwriter, long thisTime, int count) throws IOException {
		jwriter.name("fields") ;
		jwriter.beginObject() ;
		for (Entry<String, FieldIndex> entry : fieldIndexes.entrySet()) {
			jwriter.name(entry.getKey()).value(entry.getValue().toString()) ;
		}
		jwriter.endObject() ;
		
		jwriter.name("ignoreBody").value(ignoreBody) ;
		jwriter.name("ignoreIndex").value(ignoreIndex) ;
		jwriter.name("time").value(thisTime).name("count").value(count) ;
	}

	public void indexSession(IndexSession isession, TreeNodeKey tranKey, PropertyValue tranPropertyValue) throws IOException{
		isession.setIgnoreBody(ignoreBody) ;
		
		WriteDocument commitDoc = isession.newDocument(tranKey.fqnString()) ;
		commitDoc.number("time", System.currentTimeMillis()); // searcher use for lastTranInfo
		commitDoc.add(MyField.manual(EntryKey.PARENT, Fqn.TRANSACTIONS.toString(), Store.YES, Index.NOT_ANALYZED)) ;
		commitDoc.add(MyField.manual(EntryKey.VALUE, createJsonValue(tranKey, tranPropertyValue).toString(), Store.YES, Index.NOT_ANALYZED)) ;

		isession.insertDocument(commitDoc) ;
	}

	private JsonObject createJsonValue(TreeNodeKey tranKey, PropertyValue tranPropertyValue) {
		JsonObject jobj = new JsonObject();
		jobj.addProperty(EntryKey.ID, tranKey.fqnString());
		jobj.add(EntryKey.PROPS, new JsonObject().put("time", System.currentTimeMillis()).put("config",  toJson().toString()).put("tran", tranPropertyValue.stringValue()) );
		
		return jobj;
	}
	
	public static IndexWriteConfig read(JsonObject json){
		IndexWriteConfig result = new IndexWriteConfig();
		JsonObject fields = json.asJsonObject("fields");
		for(Entry<String, JsonElement> entry : fields.entrySet()){
			result.fieldIndexes.put(entry.getKey(), FieldIndex.valueOf(entry.getValue().getAsString()) ) ;
		}
		result.ignoreBody = json.asBoolean("ignoreBody") ;
		result.ignoreIndex = json.asBoolean("ignoreIndex") ;
		return result ;
	}


}
