package net.ion.craken.loaders;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.ion.craken.EntryKey;
import net.ion.framework.logging.LogBroker;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.neo.NodeCursor;
import net.ion.neo.ReadNode;
import net.ion.neo.ReadRelationship;
import net.ion.neo.ReadSession;
import net.ion.neo.TransactionJob;
import net.ion.neo.WriteNode;
import net.ion.neo.WriteRelationship;
import net.ion.neo.WriteSession;

import org.infinispan.Cache;
import org.infinispan.container.entries.ImmortalCacheValue;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.CacheLoaderMetadata;
import org.infinispan.marshall.StreamingMarshaller;

@CacheLoaderMetadata(configurationClass = NeoNodeCacheStoreConfig.class)
public class NeoNodeCacheStore extends AbstractCacheStore {
	private static final Logger log = LogBroker.getLogger(NeoNodeCacheStore.class);

	private NeoNodeCacheStoreConfig config;
	private ReadSession session;
	private static final String ValueFieldName = "__value";
	private static final String IDProp = "_id";
	private static final String JSONProp = "_json";

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = (NeoNodeCacheStoreConfig) config;
	}

	@Override
	public void start() throws CacheLoaderException {
		super.start();
		this.session = config.login();
	}

	@Override
	public void store(final InternalCacheEntry entry) throws CacheLoaderException {
		
		try {
			Boolean result = session.tran(new TransactionJob<Boolean>() {
				@Override
				public Boolean handle(WriteSession wsession) {
					try {
						WriteNode found = wsession.createQuery().parseQuery(IDProp + ":" + entry.getKey()).findOne();
						if (found == null)
							found = wsession.newNode().property(IDProp, entry.getKey());

						if (entry.canExpire()) {
							long expiry = entry.getExpiryTime();
							if (entry.getMaxIdle() > 0) {
								// Coding getExpiryTime() for transient entries has the risk of being a moving target
								// which could lead to unexpected results, hence, InternalCacheEntry calls are required
								expiry = entry.getMaxIdle() + System.currentTimeMillis();
							}
							found.property("expiry", expiry);
						}
						found.property(IDProp, entry.getKey());
						
						
						
						found.property(JSONProp, JsonParser.fromObject(entry.getValue()).toString()) ;
						
						
						found.property(ValueFieldName, marshaller.objectToByteBuffer(entry));
						return Boolean.TRUE;
					} catch (InterruptedException ex) {
						ex.printStackTrace();
						return Boolean.FALSE;
					} catch (IOException ex) {
						ex.printStackTrace();
						return Boolean.FALSE;
					}
				}
			}).get();
			
		} catch (InterruptedException e) {
			throw new CacheLoaderException(e) ;
		} catch (ExecutionException e) {
			throw new CacheLoaderException(e) ;
		}
		
	}

	@Override
	public InternalCacheEntry load(Object key) throws CacheLoaderException {
//		session.createQuery().find().toList() ;
		
		ReadNode read = session.createQuery().parseQuery(IDProp + ":" + transKey(key)).findOne();
		if (read == null) {
			return null;
		}
		InternalCacheEntry entry = toCacheEntry(read) ;
		if (entry != null && entry.isExpired(System.currentTimeMillis())) {
			return null;
		}
		return entry;
	}

	private Object transKey(Object key) {
		return (key instanceof EntryKey) ? ((EntryKey) key).get() : key;
	}

	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		Set<InternalCacheEntry> set = new LinkedHashSet<InternalCacheEntry>();
		NodeCursor<ReadNode, ReadRelationship> cursor = session.createQuery().find();
		while (cursor.hasNext()) {
			ReadNode raw = cursor.next();
			set.add(toCacheEntry(raw)) ;
		}
		return set;
	}

	@Override
	public Set<InternalCacheEntry> load(int numEntries) throws CacheLoaderException {
		Set<InternalCacheEntry> set = new LinkedHashSet<InternalCacheEntry>();
		NodeCursor<ReadNode, ReadRelationship> cursor = session.createQuery().atLength(numEntries).find();
		while (cursor.hasNext()) {
			ReadNode raw = cursor.next();
			set.add(toCacheEntry(raw)) ;
		}
		return set;
	}

	private InternalCacheEntry toCacheEntry(ReadNode raw){
//		byte[] bytes = (byte[]) raw.property(ValueFieldName);
//		Object readObject = marshaller.objectFromByteBuffer(bytes);
		
		return new ImmortalCacheValue(Employee.createEmp(20, "incache", 99)).toInternalCacheEntry(raw.property(IDProp));
	}
	
	
	@Override
	protected void purgeInternal() throws CacheLoaderException {
		session.tran(new TransactionJob<Integer>() {
			@Override
			public Integer handle(WriteSession wsession) {
				NodeCursor<WriteNode, WriteRelationship> cursor = wsession.createQuery().parseQuery("expiry:[0 TO " + System.currentTimeMillis() + "]").find();
				int count = 0;
				while (cursor.hasNext()) {
					cursor.next().remove();
					count++;
				}
				return count;
			}
		});
	}

	@Override
	public Set<Object> loadAllKeys(Set<Object> keysToExclude) throws CacheLoaderException {
		NodeCursor<ReadNode, ReadRelationship> nc = session.createQuery().find();
		Set<Object> keySet = new LinkedHashSet<Object>();
		while(nc.hasNext()){
			ReadNode node = nc.next();
			if (! keysToExclude.contains(node.property(IDProp))) keySet.add(node) ;
		}
		return keySet ;
	}

	@Override
	public void clear() throws CacheLoaderException {
		try {
			session.dropWorkspace();
		} catch (IOException e) {
			throw new CacheLoaderException(e);
		}
	}

	@Override
	public boolean remove(Object key) throws CacheLoaderException {
		Future<Boolean> future = session.tran(new TransactionJob<Boolean>() {
			@Override
			public Boolean handle(WriteSession wsession) {
				WriteNode node = wsession.createQuery().findOne();
				if (node == null)
					return false;
				else
					node.remove();
				return true;
			}
		});

		log.log(Level.INFO, "removed %s expired records", future);
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
		return NeoNodeCacheStoreConfig.class ;
	}
	
	public ReadSession session(){
		return session ;
	}
}
