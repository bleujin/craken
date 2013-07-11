package net.ion.bleujin.infinispan;

import java.io.IOException;

import net.ion.craken.loaders.lucene.LazyCentralConfig;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.index.CorruptIndexException;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.manager.CacheContainer;

public class CentralCacheStoreConfig extends AbstractCacheStoreConfig {

	private static final long serialVersionUID = 5891372400491793884L;
	public final static String Location = "location" ;
	public final static String MaxEntries = "maxEntries" ;
	public final static String ChunkSize = "chunkSize" ;
	
	
	private String location = "./resource/index";
	private int maxChunkEntries = 5;
	private int chunkSize = 1024 * 1024 * 50 ; 
	private int lockTimeoutMs = 60 * 1000 ;

	private int maxNodeEntry = 2000;
	private LazyCentralConfig lazyConfig = new LazyCentralConfig() ;

	public CentralCacheStoreConfig() {
		setCacheLoaderClassName(CentralCacheStore.class.getName());
	}
	
	public static CentralCacheStoreConfig create() {
		return new CentralCacheStoreConfig();
	}
	
	public static CentralCacheStoreConfig createDefault() {
		return CentralCacheStoreConfig.create().location("./resource/local") ;
	}
	

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		testImmutability("location");
		this.location = location;
	}

	public void setMaxChunkEntries(int maxEntries) {
		this.maxChunkEntries = maxEntries;
	}

	public void setChunkSize(int chunkSize) {
		this.chunkSize = chunkSize;
	}

	
	public String location() {
		return location;
	}

	public int maxChunkEntries() {
		return maxChunkEntries;
	}

	public int chunkSize(){
		return chunkSize ;
	}


	public CentralCacheStoreConfig location(String path) {
		setLocation(path) ;
		return this;
	}

	public CentralCacheStoreConfig maxChunkEntries(int maxEntries) {
		setMaxChunkEntries(maxEntries) ;
		return this;
	}

	public CentralCacheStoreConfig maxNodeEntry(int maxNodeEntry){
		this.maxNodeEntry = maxNodeEntry ;
		return this ;
	}
	
	public int maxNodeEntry(){
		return maxNodeEntry ;
	}
	
	public CentralCacheStoreConfig chunkSize(int chunkSize){
		setChunkSize(chunkSize) ;
		return this ;
	}

	public CentralCacheStoreConfig lockTimeoutMs(int lockTimeoutMs){
		this.lockTimeoutMs = lockTimeoutMs ;
		return this ;
	}
	
	public int lockTimeoutMs(){
		return lockTimeoutMs ;
	}
	

	
	
	public Central buildCentral() throws CorruptIndexException, IOException {
		return CentralConfig.newLocalFile().dirFile(location()).indexConfigBuilder().setRamBufferSizeMB(128).build();
	}
	
	public CentralConfig centralConfig(){
		return lazyConfig ;
	}
	
}