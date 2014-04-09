package net.ion.craken.loaders.lucene;

import java.io.Serializable;
import java.util.List;
import java.util.Map.Entry;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.common.ReadDocument;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.container.entries.InternalCacheEntry;

public class DocEntry extends ImmortalCacheEntry implements Serializable{
	private static final long serialVersionUID = 8793021912637163008L;

	public DocEntry(Object key, ImmortalCacheValue cacheValue) {
		super(key, cacheValue);
	}

	public static InternalCacheEntry create(TreeNodeKey parentKey, List<ReadDocument> docs) {
		AtomicHashMap<String, Fqn> nodeValue = new AtomicHashMap<String, Fqn>();
		for (ReadDocument doc : docs) {
			nodeValue.put(StringUtil.substringAfterLast(doc.idValue(), "/"), Fqn.fromString(doc.idValue())) ;
		}
		
		return new ImmortalCacheValue(nodeValue).toInternalCacheEntry(parentKey);
//		MortalCacheValue mv = new MortalCacheValue(nodeValue, System.currentTimeMillis(), 1000);
//		return mv.toInternalCacheEntry(parentKey) ;
//		final DocEntry create = new DocEntry(parentKey, mv);
//		return create;
	}

	
	public static InternalCacheEntry create(ReadDocument findDoc) {
		final String jsonString = findDoc.get(EntryKey.VALUE);
		if (StringUtil.isBlank(jsonString)) {
			
			return null ;
		}
		
		JsonObject raw = JsonObject.fromString(jsonString) ;
		TreeNodeKey nodeKey = TreeNodeKey.fromString(raw.asString(EntryKey.ID));

		return createDataEntry(nodeKey, raw);
	}

	// public static Collection<NodeEntry> creates(JsonObject raw) {
	// return ListUtil.toList(create(raw));
	// }

	private static InternalCacheEntry createStruEntry(TreeNodeKey nodeKey, JsonObject raw) {
		AtomicHashMap<String, Fqn> nodeValue = new AtomicHashMap<String, Fqn>();

		JsonObject props = raw.getAsJsonObject(EntryKey.PROPS);
		for (Entry<String, JsonElement> entry : props.entrySet()) {
			String pkey = entry.getKey();
			String absoluteFqn = entry.getValue().getAsString();
			nodeValue.put(pkey, Fqn.fromString(absoluteFqn));
		}
		return new ImmortalCacheValue(nodeValue).toInternalCacheEntry(nodeKey) ;
	}

	private static InternalCacheEntry createDataEntry(TreeNodeKey nodeKey, JsonObject raw) {
		AtomicHashMap<PropertyId, PropertyValue> nodeValue = new AtomicHashMap<PropertyId, PropertyValue>();

		JsonObject props = raw.getAsJsonObject(EntryKey.PROPS);
		for (Entry<String, JsonElement> entry : props.entrySet()) {
			String pkey = entry.getKey();
			JsonElement pvalue = entry.getValue();
			nodeValue.put(PropertyId.fromIdString(pkey), PropertyValue.loadFrom(nodeKey, pkey, pvalue.getAsJsonObject()));
		}

		return new ImmortalCacheValue(nodeValue).toInternalCacheEntry(nodeKey) ;
//		MortalCacheValue mvalue = new MortalCacheValue(nodeValue, lastmodified, System.currentTimeMillis());
//		final DocEntry create = new DocEntry(nodeKey, new ImmortalCacheValue(nodeValue));
//		return create;
	}



}


