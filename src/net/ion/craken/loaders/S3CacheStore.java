package net.ion.craken.loaders;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LoaderConfigurationBuilder;
import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.loaders.cloud.CloudCacheStore;
import org.infinispan.marshall.StreamingMarshaller;

public class S3CacheStore extends AbstractCacheStore {

	
	private CloudCacheStore inner = new CloudCacheStore();
	private CacheLoaderConfig config;
	
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		super.init(config, cache, m);
		this.config = config;
	}

	@Override
	public void start() throws CacheLoaderException {
		super.start();
	}

	@Override
	public void clear() throws CacheLoaderException {
		inner.clear(); 
	}

	@Override
	public void fromStream(ObjectInput oin) throws CacheLoaderException {
		inner.fromStream(oin);
	}

	@Override
	public boolean remove(Object key) throws CacheLoaderException {
		return inner.remove(key);
	}

	@Override
	public void store(InternalCacheEntry centry) throws CacheLoaderException {
		inner.store(centry);
	}

	@Override
	public void toStream(ObjectOutput oou) throws CacheLoaderException {
		inner.toStream(oou);
	}

	@Override
	public Class<? extends CacheLoaderConfig> getConfigurationClass() {
		return config.getClass();
	}

	@Override
	public InternalCacheEntry load(Object key) throws CacheLoaderException {
		return inner.load(key);
	}

	@Override
	public Set<InternalCacheEntry> load(int maxEntry) throws CacheLoaderException {
		return inner.load(maxEntry);
	}

	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		return inner.loadAll();
	}

	@Override
	public Set<Object> loadAllKeys(Set<Object> excludeKey) throws CacheLoaderException {
		return inner.loadAllKeys(excludeKey);
	}

	@Override
	protected void purgeInternal() throws CacheLoaderException {

	}


	public static Configuration s3TestConfig() throws IOException {
		return S3CacheStoreConfig.test().build();
	}

	public static Configuration s3Config(S3CacheStoreConfig s3config) {
		return s3config.build();
	}
	
}
