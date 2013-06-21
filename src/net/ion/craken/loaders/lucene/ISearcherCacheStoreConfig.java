package net.ion.craken.loaders.lucene;

import org.infinispan.loaders.AbstractCacheStoreConfig;

public class ISearcherCacheStoreConfig extends AbstractCacheStoreConfig {
	private static final long serialVersionUID = 1L;

	
	public final static String Location = "location" ;
	public final static String MaxEntries = "maxEntries" ;
	public final static String ChunkSize = "chunkSize" ;
	
	
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


	public ISearcherCacheStoreConfig location(String path) {
		setLocation(path) ;
		return this;
	}

	public ISearcherCacheStoreConfig maxEntries(int maxEntries) {
		setMaxEntries(maxEntries) ;
		return this;
	}

	public ISearcherCacheStoreConfig chunkSize(int chunkSize){
		setChunkSize(chunkSize) ;
		return this ;
	}

	
	
	public static ISearcherCacheStoreConfig create() {
		return new ISearcherCacheStoreConfig();
	}

	public static ISearcherCacheStoreConfig createDefault() {
		return ISearcherCacheStoreConfig.create().location("./resource/local").maxEntries(10).chunkSize(1024 * 1024 * 10);
	}
	
}