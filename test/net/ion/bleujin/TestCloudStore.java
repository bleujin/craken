package net.ion.bleujin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.StringUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LoaderConfigurationBuilder;
import org.infinispan.loaders.cloud.CloudCacheStore;
import org.infinispan.loaders.cloud.configuration.CloudCacheStoreConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestCloudStore extends TestCase {


	public void testSave() throws Exception {
		DefaultCacheManager dcm = new DefaultCacheManager();
		dcm.defineConfiguration("s3cache", makeConfig()) ;
		dcm.start();

		Cache<String, String> cache = dcm.getCache("s3cache");
		for (int i = 0; i < 5; i++) {
			cache.put("hello " + i, "world " + i);
		}

		cache.stop();
		dcm.stop();
	}

	public void testPut() throws Exception {
		DefaultCacheManager dcm = new DefaultCacheManager();
		dcm.defineConfiguration("employee", makeConfig()) ;
//		cache.clear() ;
		dcm.start(); 
		
		Cache<String, String> cache = dcm.getCache("employee");
		for (int i = 0; i < 10; i++) {
			cache.put("hello" + i, "Helllo");
		}

		Debug.line(cache.keySet().size()) ;
		cache.stop(); 
		dcm.stop(); 
	}

	
	public void testLoad() throws Exception {
		Configuration config = makeConfig() ;
		
		DefaultCacheManager dcm = new DefaultCacheManager();
		dcm.defineConfiguration("s3cache", config) ;
		dcm.start();

		Cache<String, String> cache = dcm.getCache("s3cache");
		Debug.line(cache.get("hello 1")) ;

		cache.stop();
		dcm.stop();
	}
	
	
	private Configuration makeConfig() {
		String accessKey = "AKIAIVKXQXFRBCJJRLPQ";
		String accessPwd = "Fof2dm0CzKiZ4alyjiyvcABMUUGhEFhROwwOyG9S" ; // ;
		String bucket = "bleujins3bucket" ; // "bleujins3bucket";

		CloudCacheStore cacheStore = new CloudCacheStore() ;
		LoaderConfigurationBuilder cbuilder = new ConfigurationBuilder().loaders().addCacheLoader().cacheStore(cacheStore).fetchPersistentState(false).ignoreModifications(false).purgeOnStartup(false) ;
		cbuilder.addProperty("identity", accessKey).addProperty("password", accessPwd).addProperty("bucketPrefix", bucket)
				.addProperty("requestTimeout", "20000").addProperty("cloudService", "aws-s3").addProperty("secure", "false").addProperty("compress", "false") ;
		cbuilder.async().enabled(true).flushLockTimeout(15000).threadPoolSize(10) ;
		
		Configuration config = cbuilder.build() ;
		return config;
	}

	
	public void testLoadFromConfig() throws Exception {
		DefaultCacheManager dcm = new DefaultCacheManager(this.getClass().getResourceAsStream("test-config.xml"), true) ;
		Cache<String, String> cache = dcm.getCache("named") ;
		
		cache.put("hello", "world") ;
		
		cache.stop();
		dcm.stop(); 
		
	}
	
	
	public void testFindJar() throws Exception {
		File dir = new File("D:/temp/infinispan-5.2.1.Final-all/infinispan-5.2.1.Final-all/lib");
		String findName = "org.infinispan.loaders.bucket";
		findFile(dir, StringUtil.replace(findName, ".", "/"));
	}

	private boolean findFile(File source, String fileName) throws IOException {
		if (source.isFile()) {
			if (source.getName().endsWith(".jar")) {
				InputStream originalInput = new FileInputStream(source);
				JarInputStream jarInput = new JarInputStream(originalInput);
				try {
					ZipEntry entry = jarInput.getNextEntry();
					while (entry != null) {
						if (entry.getName().startsWith(fileName)) {
							Debug.line(source, fileName);
							return true;
						}
						entry = jarInput.getNextEntry();
					}
				} finally {
					jarInput.close();
				}
			}
			return false;
		} else if (source.isDirectory()) {
			File[] files = source.listFiles();
			for (File file : files) {
				boolean found = findFile(file, fileName);
				if (found)
					break;
			}
		}
		return false;
	}

}
