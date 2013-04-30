package net.ion.craken.loaders;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.net.UnknownHostException;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ion.craken.EntryKey;
import net.ion.framework.logging.LogBroker;

import org.infinispan.Cache;
import org.infinispan.container.entries.InternalCacheEntry;
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
import com.mongodb.WriteConcern;
import com.mongodb.WriteResult;

@CacheLoaderMetadata(configurationClass = MongoDBCacheStoreConfig.class)
public class MongoDBCacheStore extends AbstractCacheStore {
	private static final Logger log = LogBroker.getLogger(MongoDBCacheStore.class);

	private MongoDBCacheStoreConfig config;
	private DBCollection coll;
	private static final String ValueFieldName = "__value";

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (MongoDBCacheStoreConfig) config;
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
			m.setWriteConcern(WriteConcern.SAFE);
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
		coll = m.getDB(config.getDbName()).getCollection(config.getDbCollection());
		coll.ensureIndex("expiry");
	}

	@Override
	public void store(InternalCacheEntry entry) throws CacheLoaderException {
		BasicDBObject doc = new BasicDBObject();
		if (entry.canExpire()) {
			long expiry = entry.getExpiryTime();
			if (entry.getMaxIdle() > 0) {
				// Coding getExpiryTime() for transient entries has the risk of being a moving target
				// which could lead to unexpected results, hence, InternalCacheEntry calls are required
				expiry = entry.getMaxIdle() + System.currentTimeMillis();
			}
			doc.put("expiry", expiry);
		}
		doc.put("_id", entry.getKey());
		
		try {
			entry.getValue() ;
			
			byte[] bytes = marshaller.objectToByteBuffer(entry);
			doc.put(ValueFieldName, bytes);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		
		
		WriteResult result = coll.save(doc);
		result.getLastError().throwOnError();
	}

	@Override
	public InternalCacheEntry load(Object key) throws CacheLoaderException {
		DBObject read = coll.findOne(new BasicDBObject("_id", transKey(key)));
		if (read == null) {
			return null;
		}
		byte[] bytes = (byte[]) read.get(ValueFieldName);
		try {
			InternalCacheEntry entry = (InternalCacheEntry) marshaller.objectFromByteBuffer(bytes);
			if (entry != null && entry.isExpired(System.currentTimeMillis())) {
				return null;
			}
			return entry;
		} catch (IOException e) {
			throw new RuntimeException(e);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	private Object transKey(Object key) {
		return (key instanceof EntryKey) ? ((EntryKey)key).get() : key;
	}
	
	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		Set<InternalCacheEntry> set = new LinkedHashSet<InternalCacheEntry>();
		DBCursor cursor = coll.find();
		try {
			while (cursor.hasNext()) {
				DBObject raw = cursor.next();
				byte[] bytes = (byte[]) raw.get(ValueFieldName);
				set.add((InternalCacheEntry) marshaller.objectFromByteBuffer(bytes));
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
		DBCursor cursor = coll.find();
		int i = 0;
		try {
			while (cursor.hasNext() && i++ < numEntries) {
				DBObject raw = cursor.next();
				byte[] bytes = (byte[]) raw.get(ValueFieldName);
				Object readObject = marshaller.objectFromByteBuffer(bytes);
				
				set.add((InternalCacheEntry) readObject);
			}
			return set;
		} catch (Exception e) {
			throw new CacheLoaderException(e);
		} finally {
			cursor.close();
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
		DBObject query = new BasicDBObject("_id", new BasicDBObject("$nin", keysToExclude));
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
		coll.drop();
	}

	@Override
	public boolean remove(Object key) throws CacheLoaderException {
		DBObject query = new BasicDBObject("_id", key);
		WriteResult result = coll.remove(query);
		log.log(Level.INFO, "removed %d expired records", result.getN());

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
		return MongoDBCacheStoreConfig.class;
	}
}
