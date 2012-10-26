package net.ion.craken.loaders;

import static net.ion.radon.repository.NodeConstants.ARADON;
import static net.ion.radon.repository.NodeConstants.ARADON_GROUP;
import static net.ion.radon.repository.NodeConstants.ARADON_UID;

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;
import com.mongodb.MongoOptions;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

import net.ion.craken.EntryKey;
import net.ion.craken.simple.EmanonKey;
import net.ion.framework.parse.gson.JsonElement;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.parse.gson.internal.LazilyParsedNumber;
import net.ion.framework.parse.gson.internal.Primitives;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;
import net.ion.radon.repository.AradonId;
import net.ion.radon.repository.NodeObject;
import net.ion.radon.repository.PropertyFamily;
import net.ion.radon.repository.PropertyId;
import net.ion.radon.repository.PropertyQuery;
import net.ion.radon.repository.util.JSONUtil;

import org.apache.commons.discovery.tools.ClassUtils;
import org.bson.types.ObjectId;
import org.infinispan.Cache;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.marshall.StreamingMarshaller;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.RuntimeErrorException;

@CacheLoaderMetadata(configurationClass = NewMongoDBCacheStoreConfig.class)
public class NewMongoDBCacheStore extends AbstractCacheStore {
	private NewMongoDBCacheStoreConfig config;
	private DBCollection coll;

	private static final String ValueName = "__value";
	private static final String ClassName = "__class";
	private static final String FieldsName = "fields";

	public NewMongoDBCacheStore(){
		super() ;
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
		coll.ensureIndex("expiry");

		BasicDBObject aradon_options = new BasicDBObject();
		aradon_options.put("name", "_aradon_id");
		aradon_options.put("unique", Boolean.TRUE);

		coll.ensureIndex(PropertyFamily.create(ARADON_GROUP, 1).put(ARADON_UID, -1).getDBObject(), aradon_options);
	}

	@Override
	public void store(InternalCacheEntry entry) throws CacheLoaderException {

		NodeObject nobj = NodeObject.create();
		if (entry.canExpire()) {
			long expiry = entry.getExpiryTime();
			if (entry.getMaxIdle() > 0) {
				// Coding getExpiryTime() for transient entries has the risk of being a moving target
				// which could lead to unexpected results, hence, InternalCacheEntry calls are required
				expiry = entry.getMaxIdle() + System.currentTimeMillis();
			}
			nobj.put("expiry", expiry);
		}
		final NodeObject aradonId = AradonId.create(this.cache.getName(), transKey(entry.getKey())).toNodeObject();
		nobj.put("_id", transKey(entry.getKey()));

		try {
			nobj.put(PropertyId.reserved(ARADON).getKeyString(), aradonId);
			nobj.put(ClassName, entry.getValue().getClass().getCanonicalName());
			final JsonObject jsonElement = JsonObject.fromObject(entry.getValue());
			if (jsonElement == null) {
				throw new IllegalArgumentException("not supported type : null") ;
			}
			nobj.put(FieldsName, JSONUtil.toPreferObject(jsonElement));

			entry.setValue(StringUtil.EMPTY);
			byte[] bytes = marshaller.objectToByteBuffer(entry);
			
			nobj.put(ValueName, bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		WriteResult result = coll.save(nobj.getDBObject());
		result.getLastError().throwOnError();

	}

	private Object transKey(Object key) {
		return (key instanceof EntryKey) ? ((EntryKey)key).get() : key;
	}


	@Override
	public InternalCacheEntry load(Object key) throws CacheLoaderException {

		final DBCursor nc = coll.find(new BasicDBObject("_id", transKey(key)));
		DBObject read = nc.hasNext() ? nc.next() : null;
		
		
		if (read == null) {
			return null;
		}
		byte[] bytes = (byte[]) read.get(ValueName);
		try {

			InternalCacheEntry readObject = (InternalCacheEntry) marshaller.objectFromByteBuffer(bytes);
			readObject.setValue(transObject(read));

			if (readObject != null && readObject.isExpired(System.currentTimeMillis())) {
				return null;
			}
			return readObject;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private DBObject createkeyDBObject(Object key) {
		return PropertyQuery.createByAradon(this.cache.getName(), transKey(key)).getDBObject();
		// return AradonId.create(this.cache.getName(), key).toNodeObject().getDBObject();
	}

	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		Set<InternalCacheEntry> set = new LinkedHashSet<InternalCacheEntry>();
		DBCursor cursor = coll.find(PropertyQuery.createByAradon(this.cache.getName()).getDBObject());
		try {
			while (cursor.hasNext()) {
				DBObject raw = cursor.next();
				byte[] bytes = (byte[]) raw.get(ValueName);
				final InternalCacheEntry readObject = (InternalCacheEntry) marshaller.objectFromByteBuffer(bytes);

				readObject.setValue(transObject(raw));
				set.add(readObject);
			}
			return set;
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		} finally {
			cursor.close();
		}
	}

	@Override
	public Set<InternalCacheEntry> load(int numEntries) throws CacheLoaderException {
		
		Set<InternalCacheEntry> set = new LinkedHashSet<InternalCacheEntry>();
		DBCursor cursor = coll.find(PropertyQuery.createByAradon(this.cache.getName()).getDBObject());
		int i = 0;
		try {
			while (cursor.hasNext() && i++ < numEntries) {
				DBObject raw = cursor.next();
				byte[] bytes = (byte[]) raw.get(ValueName);
				InternalCacheEntry readObject = (InternalCacheEntry) marshaller.objectFromByteBuffer(bytes);
				

				readObject.setValue(transObject(raw));
				set.add(readObject);
			}
			return set;
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		} finally {
			cursor.close();
		}
	}

	private Object transObject(DBObject raw) throws ClassNotFoundException {
		Object fieldObject = raw.get("fields");
		String clzName = ObjectUtil.toString(raw.get(ClassName));
		final JsonElement jsonElement = JsonParser.fromObject(fieldObject);
		if (jsonElement.isJsonPrimitive()) {
			if (jsonElement.getAsJsonPrimitive().getValue() instanceof LazilyParsedNumber) {
				long longValue = ((LazilyParsedNumber) jsonElement.getAsJsonPrimitive().getValue()).longValue();
				return longValue;
			} else {
				return jsonElement.getAsJsonPrimitive().getValue();
			}

		} else {

			final Class<?> clz = Class.forName(clzName);
			Object newObject = jsonElement.getAsJsonObject().getAsObject(clz);
			return newObject;
		}
	}

	@Override
	protected void purgeInternal() throws CacheLoaderException {
		DBObject query = new BasicDBObject("expiry", new BasicDBObject("$lt", System.currentTimeMillis()));
		WriteResult result = coll.remove(query);
		result.getLastError().throwOnError();
	}

	@Override
	public Set<Object> loadAllKeys(Set<Object> keysToExclude) throws CacheLoaderException {
		DBObject query = new BasicDBObject(ARADON_UID, new BasicDBObject("$nin", keysToExclude));
		DBCursor cursor = coll.find(query);
		Set<Object> keySet = new LinkedHashSet<Object>();
		while (cursor.hasNext()) {
			DBObject nextDbo = cursor.next();
			keySet.add(nextDbo);
		}
		return keySet;
	}

	@Override
	public void clear() throws CacheLoaderException {
		coll.remove(PropertyQuery.createByAradon(this.cache.getName()).getDBObject());
		// coll.drop();
	}

	@Override
	public boolean remove(Object key) throws CacheLoaderException {
		WriteResult result = coll.remove(createkeyDBObject(key));
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
