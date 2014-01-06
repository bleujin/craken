package net.ion.craken.loaders.neo;

import java.io.Serializable;
import java.util.List;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
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
			Object pvalue = findNode.getProperty(pkey);
			if (List.class.isInstance(pvalue)) {
				PropertyValue arrayValue = PropertyValue.createPrimitive(null);
				for (JsonElement jele : (JsonArray) pvalue) {
//					arrayValue.append(jele.getAsJsonPrimitive().getValue());
					if (jele.isJsonObject()){
						arrayValue.append(jele.toString()) ;
//						throw new IllegalArgumentException(" -t- ?") ;
//						arrayValue.append(BlobProxy.create(jele.getAsJsonObject().asString("fqnPath"))) ;
					} else if (jele.isJsonPrimitive() && jele.getAsJsonPrimitive().isNumber()){
						final long aslong = jele.getAsJsonPrimitive().getAsLong();
						arrayValue.append(aslong);
					} else {
						arrayValue.append(jele.getAsJsonPrimitive().getValue());
					}
				}
				nodeValue.put(PropertyId.fromIdString(pkey), arrayValue);
			} else {
				nodeValue.put(PropertyId.fromIdString(pkey), PropertyValue.createPrimitive(pvalue)) ;
//				nodeValue.put(PropertyId.fromIdString(pkey), PropertyValue.createPrimitive(pvalue.isJsonObject() ?  pvalue.getAsJsonObject().toString() : pvalue.getAsJsonPrimitive().getValue()));
			}
		}

		return new ImmortalCacheValue(nodeValue).toInternalCacheEntry(nodeKey) ;
//		MortalCacheValue mvalue = new MortalCacheValue(nodeValue, lastmodified, System.currentTimeMillis());
//		final DocEntry create = new DocEntry(nodeKey, new ImmortalCacheValue(nodeValue));
//		return create;
	}



}


