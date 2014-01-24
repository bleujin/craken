package net.ion.craken.loaders.rdb;

import java.sql.SQLException;
import java.util.List;
import java.util.Map.Entry;

import net.ion.craken.loaders.EntryKey;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.framework.db.Rows;
import net.ion.framework.parse.gson.JsonArray;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;

import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.container.entries.InternalCacheEntry;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;

public class RowEntry {

	public static InternalCacheEntry createStru(TreeNodeKey parentKey, Rows rows) throws SQLException {
		AtomicHashMap<String, Fqn> valueMap = new AtomicHashMap<String, Fqn>();
		while (rows.next()) {
			String path = rows.getString("fqn");
			valueMap.put(StringUtil.substringAfterLast(path, "/"), Fqn.fromString(path));
		}

		return new ImmortalCacheValue(valueMap).toInternalCacheEntry(parentKey);
	}

	public static InternalCacheEntry create(Rows currRow) throws SQLException {
		final String idString = currRow.getString("fqn");

		TreeNodeKey nodeKey = TreeNodeKey.fromString(idString);
		AtomicHashMap<PropertyId, PropertyValue> nodeValue = new AtomicHashMap<PropertyId, PropertyValue>();

//		findRows.first();

		JsonObject jsonProps = JsonObject.fromString(currRow.getString("props"));
		for (Entry<String, JsonElement> entry : jsonProps.entrySet()) {
			nodeValue.put(PropertyId.fromIdString(entry.getKey()), PropertyValue.loadFrom(entry.getValue().getAsJsonArray()));
		}
		return new ImmortalCacheValue(nodeValue).toInternalCacheEntry(nodeKey);
	}

}
