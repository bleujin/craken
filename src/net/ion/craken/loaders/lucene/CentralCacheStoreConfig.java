package net.ion.craken.loaders.lucene;

import java.io.File;
import java.io.IOException;

import net.ion.framework.util.FileUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.search.analyzer.MyKoreanAnalyzer;

import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.infinispan.loaders.AbstractCacheStoreConfig;

public class CentralCacheStoreConfig extends AbstractCacheStoreConfig {

	private static final long serialVersionUID = 5891372400491793884L;
	public final static String Location = "location" ;
	public final static String MaxEntries = "maxEntries" ;
	public final static String ChunkSize = "chunkSize" ;
	
	
	private String location = "./resource/index";
	private int lockTimeoutMs = 60 * 1000 ;

	private int maxNodeEntry = 30000 ;
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
		this.location = location ;
//		this.properties.put("location", location) ;
	}

	
	public CentralCacheStoreConfig resetDir() throws IOException{
		FileUtil.deleteDirectory(new File(location)) ;
		return this ;
	}
	
	public String location() {
		return location;
	}

	public CentralCacheStoreConfig location(String path) {
		setLocation(path) ;
		return this;
	}


	public CentralCacheStoreConfig maxNodeEntry(int maxNodeEntry){
		this.maxNodeEntry = maxNodeEntry ;
		return this ;
	}
	
	public int maxNodeEntry(){
		return maxNodeEntry ;
	}


	public CentralCacheStoreConfig lockTimeoutMs(int lockTimeoutMs){
		this.lockTimeoutMs = lockTimeoutMs ;
		return this ;
	}
	
	public int lockTimeoutMs(){
		return lockTimeoutMs ;
	}
	

	
	
	public Central buildCentral() throws CorruptIndexException, IOException {
		Directory dir = null ;
		if (StringUtil.isBlank(location)) {
			dir = new RAMDirectory() ;
		} else {
			final File file = new File(location);
			if (! file.exists()) file.mkdirs() ;
			dir = FSDirectory.open(file) ;
		}
		final Central result = lazyConfig.dir(dir).indexConfigBuilder().indexAnalyzer(new MyKoreanAnalyzer()).parent().searchConfigBuilder().queryAnalyzer(new MyKoreanAnalyzer()).build();
		
//		Debug.line('i', this.hashCode(), this) ;
		
		return result ;
//		CentralConfig.newLocalFile().dirFile(location()).indexConfigBuilder().setRamBufferSizeMB(128).build();
	}
	
	public CentralConfig centralConfig(){
		return lazyConfig ;
	}
	
}