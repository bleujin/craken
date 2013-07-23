package net.ion.craken.loaders.lucene;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;

import net.ion.craken.io.BlobProxy;
import net.ion.craken.io.BlobValue;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.ReadDocument;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.MortalCacheEntry;
import org.infinispan.container.entries.MortalCacheValue;

public class DocEntry extends ImmortalCacheEntry implements Serializable{
	private static final long serialVersionUID = 8793021912637163008L;

	public static final String VALUE = "__value";

	public static final String ID = "__id";
	public static final String LASTMODIFIED = "__lastmodified";
	public static final String PROPS = "__props";

	public static final String PARENT = "__parent";

	public DocEntry(Object key, ImmortalCacheValue cacheValue) {
		super(key, cacheValue);
	}

	public static InternalCacheEntry create(TreeNodeKey parentKey, List<ReadDocument> docs) {
		AtomicHashMap<String, Fqn> nodeValue = new AtomicHashMap<String, Fqn>();
		for (ReadDocument doc : docs) {
			nodeValue.put(StringUtil.substringAfterLast(doc.docId(), doc.get(PARENT)), Fqn.fromString(doc.docId())) ;
		}
		
		return new ImmortalCacheValue(nodeValue).toInternalCacheEntry(parentKey);
//		MortalCacheValue mv = new MortalCacheValue(nodeValue, System.currentTimeMillis(), 1000);
//		return mv.toInternalCacheEntry(parentKey) ;
//		final DocEntry create = new DocEntry(parentKey, mv);
//		return create;
	}

	
	public static InternalCacheEntry create(ReadDocument findDoc) {
		final String jsonString = findDoc.get(DocEntry.VALUE);
		if (StringUtil.isBlank(jsonString)) return null ;
		
		JsonObject raw = JsonObject.fromString(jsonString) ;
		TreeNodeKey nodeKey = TreeNodeKey.fromString(raw.asString(ID));

		return createDataEntry(nodeKey, raw);
	}

	// public static Collection<NodeEntry> creates(JsonObject raw) {
	// return ListUtil.toList(create(raw));
	// }

	private static InternalCacheEntry createStruEntry(TreeNodeKey nodeKey, JsonObject raw) {
		long lastmodified = Long.parseLong(raw.asString(LASTMODIFIED));
		AtomicHashMap<String, Fqn> nodeValue = new AtomicHashMap<String, Fqn>();

		JsonObject props = raw.getAsJsonObject(PROPS);
		for (Entry<String, JsonElement> entry : props.entrySet()) {
			String pkey = entry.getKey();
			String absoluteFqn = entry.getValue().getAsString();
			nodeValue.put(pkey, Fqn.fromString(absoluteFqn));
		}
		return new ImmortalCacheValue(nodeValue).toInternalCacheEntry(nodeKey) ;
//		final DocEntry create = new DocEntry(nodeKey, new ImmortalCacheValue(nodeValue));
//		MortalCacheValue mvalue = new MortalCacheValue(nodeValue, System.currentTimeMillis(), 10 * 1000);
//		final DocEntry create = new DocEntry(nodeKey, mvalue);
//		return create;
	}

	private static InternalCacheEntry createDataEntry(TreeNodeKey nodeKey, JsonObject raw) {
		long lastmodified = Long.parseLong(raw.asString(LASTMODIFIED));
		AtomicHashMap<PropertyId, PropertyValue> nodeValue = new AtomicHashMap<PropertyId, PropertyValue>();

		JsonObject props = raw.getAsJsonObject(PROPS);
		for (Entry<String, JsonElement> entry : props.entrySet()) {
			String pkey = entry.getKey();
			JsonElement pvalue = entry.getValue();
			if (pvalue.isJsonArray()) {
				PropertyValue arrayValue = PropertyValue.createPrimitive(null);
				for (JsonElement jele : (JsonArray) pvalue) {
//					arrayValue.append(jele.getAsJsonPrimitive().getValue());
					if (jele.isJsonObject()){
						arrayValue.append(BlobProxy.create(jele.getAsJsonObject().asString("fqnPath"))) ;
					} else if (jele.isJsonPrimitive() && jele.getAsJsonPrimitive().isNumber()){
						final long aslong = jele.getAsJsonPrimitive().getAsLong();
						arrayValue.append(aslong);
					} else {
						arrayValue.append(jele.getAsJsonPrimitive().getValue());
					}
				}
				nodeValue.put(PropertyId.fromIdString(pkey), arrayValue);
			} else {
				nodeValue.put(PropertyId.fromIdString(pkey), PropertyValue.createPrimitive(pvalue.getAsJsonPrimitive().getValue()));
			}
		}

		return new ImmortalCacheValue(nodeValue).toInternalCacheEntry(nodeKey) ;
//		MortalCacheValue mvalue = new MortalCacheValue(nodeValue, lastmodified, System.currentTimeMillis());
//		final DocEntry create = new DocEntry(nodeKey, new ImmortalCacheValue(nodeValue));
//		return create;
	}



}


