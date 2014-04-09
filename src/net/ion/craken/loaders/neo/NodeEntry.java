package net.ion.craken.loaders.neo;

import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.PropertyValue.VType;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.container.entries.ImmortalCacheEntry;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.container.entries.InternalCacheEntry;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class NodeEntry extends ImmortalCacheEntry implements Serializable{
	private static final long serialVersionUID = 8793021912637163008L;

	public NodeEntry(Object key, ImmortalCacheValue cacheValue) {
		super(key, cacheValue);
	}

	public static InternalCacheEntry createStru(TreeNodeKey parentKey, Iterable<Relationship> rels) {
		AtomicHashMap<String, Fqn> valueMap = new AtomicHashMap<String, Fqn>();
		for (Relationship rel : rels) {
			Node child = rel.getEndNode();
			String path = ObjectUtil.toString(child.getProperty(EntryKey.ID)) ;
			valueMap.put(StringUtil.substringAfterLast(path, "/"), Fqn.fromString(path)) ;
		}
		
		return new ImmortalCacheValue(valueMap).toInternalCacheEntry(parentKey);
//		MortalCacheValue mv = new MortalCacheValue(nodeValue, System.currentTimeMillis(), 1000);
//		return mv.toInternalCacheEntry(parentKey) ;
//		final DocEntry create = new DocEntry(parentKey, mv);
//		return create;
	}

	
	public static InternalCacheEntry create(Node findNode) {
		final String idString = ObjectUtil.toString(findNode.getProperty(EntryKey.ID));
		if (idString == null) {
			return null ;
		}
		
		TreeNodeKey nodeKey = TreeNodeKey.fromString(idString);
		return createDataEntry(nodeKey, findNode);
	}


	private static InternalCacheEntry createDataEntry(TreeNodeKey nodeKey, Node findNode) {
		AtomicHashMap<PropertyId, PropertyValue> nodeValue = new AtomicHashMap<PropertyId, PropertyValue>();

		for (String pkey : findNode.getPropertyKeys()) {
			if (pkey.startsWith("__")) continue ; // __id, __lastmodified
			
			Object pvalue = findNode.getProperty(pkey);

			JsonObject json = JsonObject.create() ;
			json.addProperty("vtype", VType.UNKNOWN.name());
			int length = Array.getLength(pvalue) ;
			JsonArray jarray = new JsonArray() ;
			json.add("vals", jarray) ;
			Debug.line(pvalue, pvalue.getClass());
			for(int i = 0 ; i <length ; i++){
				Object obj = Array.get(pvalue, i);
				jarray.adds(obj) ;
				if (i == 0) json.addProperty("vtype", PropertyValue.VType.findType(obj).name()) ;
			}
			nodeValue.put(PropertyId.fromIdString(pkey), PropertyValue.loadFrom(nodeKey, pkey, json));
		}

		return new ImmortalCacheValue(nodeValue).toInternalCacheEntry(nodeKey) ;
//		MortalCacheValue mvalue = new MortalCacheValue(nodeValue, lastmodified, System.currentTimeMillis());
//		final DocEntry create = new DocEntry(nodeKey, new ImmortalCacheValue(nodeValue));
//		return create;
	}



}


