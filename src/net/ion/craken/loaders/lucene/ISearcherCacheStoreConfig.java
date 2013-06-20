package net.ion.craken.loaders.lucene;

import org.infinispan.loaders.AbstractCacheStoreConfig;

public class ISearcherCacheStoreConfig extends AbstractCacheStoreConfig {
	private static final long serialVersionUID = 1L;

	private String location = "./resource/index";
	private int maxEntries = 10;
	private int chunkSize = 1024 * 1024 * 10 ; 
	

	public ISearcherCacheStoreConfig() {
		setCacheLoaderClassName(ISearcherCacheStore.class.getName());
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		testImmutability("location");
		this.location = location;
	}

	public ISearcherCacheStoreConfig location(String location) {
		setLocation(location);
		return this;
	}

	public int getMaxEntries() {
		return maxEntries;
	}

	public void setMaxEntries(int maxEntries) {
		this.maxEntries = maxEntries;
	}

	public ISearcherCacheStoreConfig maxEntries(int maxEntries) {
		setMaxEntries(maxEntries);
		return this;
	}
}