package net.ion.craken.loaders;

import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.infinispan.container.entries.InternalCacheEntry;
import org.infinispan.loaders.AbstractCacheStore;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.CacheLoaderException;

public class DirCacheStore extends AbstractCacheStore {

	@Override
	protected void purgeInternal() throws CacheLoaderException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void clear() throws CacheLoaderException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void fromStream(ObjectInput objectinput) throws CacheLoaderException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean remove(Object obj) throws CacheLoaderException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void store(InternalCacheEntry internalcacheentry) throws CacheLoaderException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void toStream(ObjectOutput objectoutput) throws CacheLoaderException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Class<? extends CacheLoaderConfig> getConfigurationClass() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public InternalCacheEntry load(Object obj) throws CacheLoaderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<InternalCacheEntry> load(int i) throws CacheLoaderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<InternalCacheEntry> loadAll() throws CacheLoaderException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Object> loadAllKeys(Set<Object> arg0) throws CacheLoaderException {
		// TODO Auto-generated method stub
		return null;
	}

}
