package net.ion.craken.loaders.lucene;

import org.infinispan.Cache;
import org.infinispan.loaders.CacheLoader;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;
import org.infinispan.marshall.StreamingMarshaller;

public abstract class AbCacheLoader implements CacheLoader {

	protected volatile StreamingMarshaller marshaller;
	private volatile Cache<Object, Object> cache;

	@Override
	public boolean containsKey(Object key) throws CacheLoaderException {
		return load(key) != null;
	}

	@Override
	public void init(CacheLoaderConfig config, Cache<?, ?> cache, StreamingMarshaller m) throws CacheLoaderException {
		this.marshaller = m;
		if (config == null)
			throw new IllegalStateException("Null config!!!");
		this.cache = (Cache<Object, Object>) cache;
	}

	
	protected Cache<Object, Object> cache(){
		return cache ;
	}
	
}
