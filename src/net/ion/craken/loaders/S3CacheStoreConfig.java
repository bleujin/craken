package net.ion.craken.loaders;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LoaderConfigurationBuilder;
import org.infinispan.loaders.cloud.CloudCacheStore;

// http://docs.jboss.org/infinispan/5.3/configdocs/infinispan-cachestore-cloud-config-5.3.html
public class S3CacheStoreConfig {
	private int maxEntries = 5000 ;

	private Properties props = new Properties() ;
	private S3CacheStoreConfig() {
		props.put("cloudService", "aws-s3") ;
	}

	public final static S3CacheStoreConfig create(){
		S3CacheStoreConfig result = new S3CacheStoreConfig();
		return result ; 
	}
	
	public final static S3CacheStoreConfig test() throws IOException{
		S3CacheStoreConfig result = new S3CacheStoreConfig();
		result.props.load(new FileInputStream(new File("./resource/config/s3.prop")));
		return result ;
	}
	
	
	public void maxEntries(int maxEntries) {
		this.maxEntries = maxEntries;
	}

	public int maxEntries() {
		return maxEntries;
	}
	
	public S3CacheStoreConfig accessKey(String accessKey){
		props.put("identity", accessKey) ;
		return this ;
	}
	
	public S3CacheStoreConfig password(String password){
		props.put("password", password) ;
		return this ;
	}
	
	public S3CacheStoreConfig bucketPrefix(String bucketPrefix){
		props.put("bucketPrefix", bucketPrefix) ;
		return this ;
	}
	
	public S3CacheStoreConfig requestTimeout(String timeoutMs){
		props.put("requestTimeout", timeoutMs) ;
		return this ;
	}
	
	public S3CacheStoreConfig secure(String secure){
		props.put("secure", secure) ;
		return this ;
	}
	
	public S3CacheStoreConfig compress(String compress){
		props.put("compress", compress) ;
		return this ;
	}
	

	Configuration build() {
		CloudCacheStore cacheStore = new CloudCacheStore() ;
		LoaderConfigurationBuilder cbuilder = new ConfigurationBuilder().eviction().maxEntries(maxEntries)
					.loaders().addCacheLoader().cacheStore(cacheStore)
					.fetchPersistentState(false).ignoreModifications(false).purgeOnStartup(false)
					.withProperties(props) ;
		cbuilder.async().enabled(true).flushLockTimeout(1500).threadPoolSize(5) ;
		
		return cbuilder.build() ;		
	}

}
