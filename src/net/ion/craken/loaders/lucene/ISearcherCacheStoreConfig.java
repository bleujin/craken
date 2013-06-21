package net.ion.craken.loaders.lucene;

import org.infinispan.loaders.AbstractCacheStoreConfig;

public class ISearcherCacheStoreConfig extends AbstractCacheStoreConfig {
	private static final long serialVersionUID = 1L;

	
	public final String Location = "location" ;
	public final String MaxEntries = "maxEntries" ;
	public final String ChunkSize = "chunkSize" ;
	
	
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

	public void setMaxEntries(int maxEntries) {
		this.maxEntries = maxEntries;
	}

	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	
	public String location() {
		return location;
	}

	public int maxEntries() {
		return maxEntries;
	}

	public int chunkSize(){
		return chunkSize ;
	}
	
}