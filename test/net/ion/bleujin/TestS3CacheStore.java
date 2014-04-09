package net.ion.bleujin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import junit.framework.TestCase;
import net.ion.craken.loaders.S3CacheStore;
import net.ion.craken.loaders.S3CacheStoreConfig;
import net.ion.framework.util.Debug;
import net.ion.framework.util.StringUtil;
import net.ion.radon.impl.util.CsvReader;

import org.bson.types.ObjectId;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LoaderConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.loaders.cloud.CloudCacheStore;
import org.infinispan.manager.DefaultCacheManager;

public class TestS3CacheStore extends TestCase {

	private GlobalConfiguration globalConf;
	private DefaultCacheManager dftManager;
	private Cache<String, String> cache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("infinispan-test-cluster").addProperty("configurationFile", "resource/config/jgroups-udp.xml").build();
		dftManager = new DefaultCacheManager(globalConf, S3CacheStore.s3TestConfig());
		dftManager.start() ;
		this.cache = dftManager.getCache("s3cache");
	}
	
	@Override
	protected void tearDown() throws Exception {
		dftManager.stop();
		super.tearDown();
	}

	public void testPut() throws Exception {
//		cache.clear() ;
		for (int i = 0; i < 100; i++) {
			cache.put("test" + i, "Helllo");
		}

		Debug.line(cache.keySet().size()) ;
	}
	
	
	public void testDelete() throws Exception {
//		cache.clear() ;
		for (int i = 0; i < 10; i++) {
			cache.remove("test" + i);
		}
		Debug.line(cache.keySet().size()) ;
	}
	
	public void testGet() throws Exception {
		Debug.line(cache.keySet().size());
	}
	
	
	public void testSpeed() throws Exception {
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;

		int max = 900 ;
		while(line != null && line.length > 0 && max-- > 0 ){
			cache.put("test" + max, StringUtil.join(line)) ;
		}
		
		Debug.line(cache.keySet(), cache.keySet().size());
	}
	
	

}
