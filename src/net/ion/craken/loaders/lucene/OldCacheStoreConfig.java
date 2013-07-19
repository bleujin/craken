package net.ion.craken.loaders.lucene;

import java.io.IOException;

import net.ion.framework.util.Debug;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.Directory;
import org.infinispan.Cache;
import org.infinispan.loaders.AbstractCacheStoreConfig;
import org.infinispan.lucene.InfinispanDirectory;
import org.infinispan.manager.CacheContainer;

public class OldCacheStoreConfig extends AbstractCacheStoreConfig {

	private static final long serialVersionUID = 6447733241304613130L;
	public final static String Location = "location" ;
	public final static String MaxEntries = "maxEntries" ;
	public final static String ChunkSize = "chunkSize" ;
	
	
	private String location = "./resource/index";
	private int maxChunkEntries = 5;
	private int chunkSize = 1024 * 1024 * 50 ; 
	private int lockTimeoutMs = 60 * 1000 ;

	private int maxNodeEntry = 2000;
	private LazyCentralConfig lazyConfig = new LazyCentralConfig() ;
	private Central central = null ;

	public OldCacheStoreConfig() {
		setCacheLoaderClassName(OldCacheStore.class.getName());
	}
	
	public static OldCacheStoreConfig create() {
		return new OldCacheStoreConfig();
	}
	
	public static OldCacheStoreConfig createDefault() {
		return OldCacheStoreConfig.create().location("./resource/local") ;
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


	public OldCacheStoreConfig location(String path) {
		setLocation(path) ;
		return this;
	}

	public OldCacheStoreConfig maxChunkEntries(int maxEntries) {
		setMaxChunkEntries(maxEntries) ;
		return this;
	}

	public OldCacheStoreConfig maxNodeEntry(int maxNodeEntry){
		this.maxNodeEntry = maxNodeEntry ;
		return this ;
	}
	
	public int maxNodeEntry(){
		return maxNodeEntry ;
	}
	
	public OldCacheStoreConfig chunkSize(int chunkSize){
		setChunkSize(chunkSize) ;
		return this ;
	}

	public OldCacheStoreConfig lockTimeoutMs(int lockTimeoutMs){
		this.lockTimeoutMs = lockTimeoutMs ;
		return this ;
	}
	
	public int lockTimeoutMs(){
		return lockTimeoutMs ;
	}
	

	
	
	public synchronized Central buildCentral(String wsname, CacheContainer dm) throws CorruptIndexException, IOException {
		if (central == null){
//			this.central = CentralConfig.newLocalFile().dirFile("./resource/file").indexConfigBuilder().setRamBufferSizeMB(128).build();
//			this.central = lazyConfig.dir(createDir(wsname, dm)).build();
			this.central = CentralConfig.oldFromDir(createDir(wsname, dm)).build() ;
		}
		return central ;
	}
	
	public CentralConfig centralConfig(){
		return lazyConfig ;
	}
	
	private Directory createDir(String wsname, CacheContainer dftManager) {
		final Cache<Object, Object> metaCache = dftManager.getCache(wsname + ".meta");
		final Cache<Object, Object> chunkCache = dftManager.getCache(wsname + ".chunks");
		final Cache<Object, Object> lockCache = dftManager.getCache(wsname + ".locks");
		
		metaCache.start() ;
		chunkCache.start() ;
		lockCache.start() ;

//		Directory dir = new DirectoryBuilderImpl(metaCache, chunkCache, lockCache, wsname).chunkSize(1024 * 64).create(); // .chunkSize()
		InfinispanDirectory dir = new InfinispanDirectory(metaCache, chunkCache, lockCache, wsname, this.chunkSize());
		
//		String location = config.getLocation();
//		if (location == null || location.trim().length() == 0)
//			location = "Infinispan-FileCacheStore";
//		File dir = new File(location);
//		if (!dir.exists() && !dir.mkdirs())
//			throw new ConfigurationException("Directory " + dir.getAbsolutePath() + " does not exist and cannot be created!");
		return dir;
	}
	
	
	
}