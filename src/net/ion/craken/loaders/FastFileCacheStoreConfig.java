package net.ion.craken.loaders;

import org.infinispan.loaders.AbstractCacheStoreConfig;

/**
 * Configures {@link FastFileCacheStore}.
 * <p/>
 * <ul>
 * <li><tt>location</tt> - a location on disk where the store can write internal files. This defaults to <tt>Infinispan-FileCacheStore</tt> in the current working directory.</li>
 * <li><tt>maxEntries</tt> - maximum number of entries allowed in the cache store. If more entries are added, the least recently used (LRU) entry is removed.</li>
 * </ul>
 * 
 * @author Karsten Blees
 */
public class FastFileCacheStoreConfig extends AbstractCacheStoreConfig {
	private static final long serialVersionUID = 1L;

	private String location = "./resource/ffcachestore";

	private int maxEntries = -1;

	public FastFileCacheStoreConfig() {
		setCacheLoaderClassName(FastFileCacheStore.class.getName());
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		testImmutability("location");
		this.location = location;
	}

	public FastFileCacheStoreConfig location(String location) {
		setLocation(location);
		return this;
	}

	public int getMaxEntries() {
		return maxEntries;
	}

	public void setMaxEntries(int maxEntries) {
		testImmutability("maxEntries");
		this.maxEntries = maxEntries;
	}

	public FastFileCacheStoreConfig maxEntries(int maxEntries) {
		setMaxEntries(maxEntries);
		return this;
	}
}