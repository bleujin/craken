package net.ion.craken.loaders;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;
import net.ion.craken.tree.TreeNodeKey.Type;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;

import org.bson.types.BasicBSONList;
import org.infinispan.Cache;
import org.infinispan.atomic.AtomicHashMap;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.container.entries.MortalCacheEntry;
import org.infinispan.container.entries.MortalCacheValue;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.marshall.StreamingMarshaller;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteResult;

@CacheLoaderMetadata(configurationClass = NewMongoDBCacheStoreConfig.class)
public class NewMongoDBCacheStore extends AbstractCacheStore {
	private NewMongoDBCacheStoreConfig config;
	private DBCollection coll;

	// private static final String ValueName = "__value";
	// private static final String ClassName = "__class";
	// private static final String FieldsName = "fields";

	public NewMongoDBCacheStore() {
		super();
	}

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (NewMongoDBCacheStoreConfig) config;
	}

	@Override
	public void start() throws CacheLoaderException {
		super.start();
		Mongo m;
		try {
			MongoOptions moptions = new MongoOptions();
			moptions.autoConnectRetry = true;
			moptions.connectionsPerHost = 100;
			moptions.threadsAllowedToBlockForConnectionMultiplier = 10;
			ServerAddress srvAddr = new ServerAddress(config.getHost(), config.getPort());
			m = new Mongo(srvAddr, moptions);
			// setting WriteConcern to true enables fsync, however performance degradation is very big: 5-10 times!
			// It makes sense to enable it only on particular updates (1.4ms vs 12ms fsynced per 1KB update)
			// m.setWriteConcern(WriteConcern.SAFE);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		coll = m.getDB(config.getDbName()).getCollection(config.getDbCollection());
		coll.ensureIndex("_expiry");
	}

	@Override
	public void store(InternalCacheEntry entry) throws CacheLoaderException {

		TreeNodeKey nodeKey = (TreeNodeKey) entry.getKey();

		Map outerMap = MapUtil.newMap();
		if (nodeKey.getType() == TreeNodeKey.Type.STRUCTURE) {
			outerMap.put("_id", "@" + nodeKey.getFqn().toString());
			for (Entry<String, Fqn> childEle : ((AtomicHashMap<String, Fqn>) entry.getValue()).entrySet()) {
				outerMap.put(childEle.getKey(), childEle.getValue().toString());
			}

		} else {

			AtomicHashMap<PropertyId, PropertyValue> nodeValue = (AtomicHashMap<PropertyId, PropertyValue>) entry.getValue();
			Map<String, Object> properties = MapUtil.newMap();
			Map<String, Set> refs = MapUtil.newMap();
			for (Entry<PropertyId, PropertyValue> ventry : nodeValue.entrySet()) {
				final PropertyId pid = ventry.getKey();
				final PropertyValue pvalue = ventry.getValue();
				if (pid.type() == PropertyId.PType.NORMAL) {
					properties.put(pid.getString(), pvalue.size() == 1 ? pvalue.value() : pvalue.asSet());
				} else {
					refs.put(pid.getString(), pvalue.asSet());
				}
			}
			outerMap.put("properties", properties);
			outerMap.put("references", refs);
			outerMap.put("_id", nodeKey.getFqn().toString());
		}

		outerMap.put("_lastmodified", new Date().getTime());
		if (entry.canExpire()) {
			long expiry = entry.getExpiryTime();
			if (entry.getMaxIdle() > 0) {
				// Coding getExpiryTime() for transient entries has the risk of being a moving target
				// which could lead to unexpected results, hence, InternalCacheEntry calls are required
				expiry = entry.getMaxIdle() + System.currentTimeMillis();
			}
			outerMap.put("_expiry", expiry);
		}

		BasicDBObject dbo = new BasicDBObject(outerMap);
		WriteResult result = coll.save(dbo);
		result.getLastError().throwOnError();

	}

	@Override
	public InternalCacheEntry load(Object _key) throws CacheLoaderException {

		TreeNodeKey key =  (_key instanceof TreeNodeKey) ? (TreeNodeKey) _key : createTreekey((BasicDBObject)_key);
		final DBCursor nc = coll.find(createDBObjectKey(key));

//		BasicDBObject key = (BasicDBObject) _key;
//		final DBCursor nc = coll.find(new BasicDBObject("_id", key.get("_id")));
		DBObject read = nc.hasNext() ? nc.next() : null;

		if (read == null) {
			return null;
		}
		InternalCacheEntry readObject = NodeEntry.create(key, read);
		if (readObject != null && readObject.isExpired(System.currentTimeMillis())) {
			return null;
		}
		return readObject;
	}

	private DBObject createDBObjectKey(TreeNodeKey key) {
		return key.getType() == Type.DATA ? new BasicDBObject("_id", key.getFqn().toString()) : new BasicDBObject("_id", "@" + key.getFqn().toString());
		// return AradonId.create(this.cache.getName(), key).toNodeObject().getDBObject();
	}
	
	private TreeNodeKey createTreekey(BasicDBObject key) {
		String id = ObjectUtil.toString(key.get("_id")) ;
		if (id.startsWith("@")) {
			return new TreeNodeKey(Fqn.fromString(id.substring(1)), Type.STRUCTURE) ;
		} else {
			return new TreeNodeKey(Fqn.fromString(id), Type.DATA) ;
		}
	}

	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		Set<InternalCacheEntry> set = new LinkedHashSet<InternalCacheEntry>();
		DBCursor cursor = coll.find();
		try {
			while (cursor.hasNext()) {
				DBObject raw = cursor.next();
				set.addAll(NodeEntry.create(raw));
			}
			return set;
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		} finally {
			cursor.close();
		}
	}

	// private InternalCacheEntry toInternalCacheEntry(DBObject raw) {
	//		
	// return NodeEntry.create(raw) ;
	//		
	//		
	// byte[] bytes = (byte[]) raw.get(ValueName);
	// final InternalCacheEntry readObject = (InternalCacheEntry) marshaller.objectFromByteBuffer(bytes);
	//
	// readObject.setValue(raw);
	// return readObject ;
	// }

	@Override
	public Set<InternalCacheEntry> load(int numEntries) throws CacheLoaderException {

		Set<InternalCacheEntry> set = new LinkedHashSet<InternalCacheEntry>();
		DBCursor cursor = coll.find();
		int i = 0;
		try {
			while (cursor.hasNext() && i++ < numEntries) {
				DBObject raw = cursor.next();
				set.addAll(NodeEntry.create(raw));
			}
			return set;
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		} finally {
			cursor.close();
		}
	}

	// private Object transObject(DBObject raw) throws ClassNotFoundException {
	// Object fieldObject = raw.get("fields");
	// String clzName = ObjectUtil.toString(raw.get(ClassName));
	// final JsonElement jsonElement = JsonParser.fromObject(fieldObject);
	// if (jsonElement.isJsonPrimitive()) {
	// if (jsonElement.getAsJsonPrimitive().getValue() instanceof LazilyParsedNumber) {
	// long longValue = ((LazilyParsedNumber) jsonElement.getAsJsonPrimitive().getValue()).longValue();
	// return longValue;
	// } else {
	// return jsonElement.getAsJsonPrimitive().getValue();
	// }
	//
	// } else {
	//
	// final Class<?> clz = Class.forName(clzName);
	// Object newObject = jsonElement.getAsJsonObject().getAsObject(clz);
	// return newObject;
	// }
	// }

	@Override
	protected void purgeInternal() throws CacheLoaderException {
		DBObject query = new BasicDBObject("_expiry", new BasicDBObject("$lt", System.currentTimeMillis()));
		WriteResult result = coll.remove(query);
		result.getLastError().throwOnError();
	}

	@Override
	public Set<Object> loadAllKeys(Set<Object> keysToExclude) throws CacheLoaderException {

		DBCursor cursor = null ;
		if (keysToExclude == null || keysToExclude.isEmpty()) {
			cursor = coll.find();
		} else {
			Set<String> fqnSet = SetUtil.newSet();
			for (Object obj : keysToExclude) {
				fqnSet.add(((TreeNodeKey) obj).getFqn().toString());
			}

			DBObject query = new BasicDBObject("_id", new BasicDBObject("$nin", fqnSet));
			cursor = coll.find(query);
		}
		Set<Object> keySet = new LinkedHashSet<Object>();
		while (cursor.hasNext()) {
			DBObject nextDbo = cursor.next();
			keySet.add(nextDbo);
		}
		return keySet;
	}

	@Override
	public void clear() throws CacheLoaderException {
		coll.drop();
	}

	@Override
	public boolean remove(Object key) throws CacheLoaderException {
		WriteResult result = coll.remove(createDBObjectKey((TreeNodeKey) key));
		result.getLastError().throwOnError();
		return false;
	}

	@Override
	public void fromStream(ObjectInput inputStream) throws CacheLoaderException {
		new UnsupportedOperationException("fromStream").printStackTrace();
	}

	@Override
	public void toStream(ObjectOutput outputStream) throws CacheLoaderException {
		new UnsupportedOperationException("toStream").printStackTrace();
	}

	@Override
	public Class<? extends CacheLoaderConfig> getConfigurationClass() {
		return NewMongoDBCacheStoreConfig.class;
	}
}

class NodeEntry extends MortalCacheEntry {

	protected NodeEntry(Object key, MortalCacheValue cacheValue) {
		super(key, cacheValue);
	}

	public static InternalCacheEntry create(TreeNodeKey key, DBObject raw) {
		if (key.getType() == Type.DATA)
			return createDataEntry(raw);
		else
			return createStruEntry(raw);
	}

	public static Collection<NodeEntry> create(DBObject raw) {
		if (raw.get("_id").toString().startsWith("@")) {
			return ListUtil.toList(createStruEntry(raw));
		} else {
			return ListUtil.toList(createDataEntry(raw));
		}
	}

	private static NodeEntry createStruEntry(DBObject raw) {

		TreeNodeKey nodeKey = new TreeNodeKey(Fqn.fromString(raw.get("_id").toString()), Type.STRUCTURE);
		long lastmodified = Long.parseLong(raw.get("_lastmodified").toString());
		AtomicHashMap<String, Fqn> nodeValue = new AtomicHashMap<String, Fqn>();

		for (String pkey : raw.keySet()) {
			if ("_id".equals(pkey) || "_lastmodified".equals(pkey))
				continue;
			String absoluteFqn = raw.get(pkey).toString();
			nodeValue.put(pkey, Fqn.fromString(absoluteFqn));
		}

		MortalCacheValue mvalue = new MortalCacheValue(nodeValue, lastmodified, System.currentTimeMillis());
		final NodeEntry create = new NodeEntry(nodeKey, mvalue);
		return create;
	}

	private static NodeEntry createDataEntry(DBObject raw) {
		final String idString = raw.get("_id").toString();

		TreeNodeKey nodeKey = new TreeNodeKey(Fqn.fromString(idString), Type.DATA);
		long lastmodified = Long.parseLong(raw.get("_lastmodified").toString());
		AtomicHashMap<PropertyId, PropertyValue> nodeValue = new AtomicHashMap<PropertyId, PropertyValue>();

		DBObject props = (DBObject) raw.get("properties");
		for (String pkey : props.keySet()) {
			Object pvalue = props.get(pkey);
			if (pvalue instanceof BasicBSONList) {
				PropertyValue arrayValue = PropertyValue.createPrimitive(null);
				for (Object ele : (BasicBSONList) pvalue) {
					arrayValue.append(ele);
				}
				nodeValue.put(PropertyId.normal(pkey), arrayValue);
			} else {
				nodeValue.put(PropertyId.normal(pkey), PropertyValue.createPrimitive(pvalue));
			}
		}

		MortalCacheValue mvalue = new MortalCacheValue(nodeValue, lastmodified, System.currentTimeMillis());
		final NodeEntry create = new NodeEntry(nodeKey, mvalue);
		return create;
	}

}