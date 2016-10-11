package net.ion.ics6.dfile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.ClusteringConfigurationBuilder;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.io.GridFile.Metadata;
import org.infinispan.io.GridFilesystem;
import org.infinispan.manager.DefaultCacheManager;

import net.ion.framework.dio.FSDataInputStream;
import net.ion.framework.dio.FSError;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import junit.framework.TestCase;

public class TestSingle extends TestCase {
	
	public void testRun() throws Exception {
		System.setProperty("java.net.preferIPv4Stack", "true") ;
		final FileServer fserver = new FileServer().runner(9200);
		
		Runtime.getRuntime().addShutdownHook(new Thread(){
			public void run(){
				try {
					fserver.shutdown();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		});
		
		Debug.line("started");
		new InfinityThread().startNJoin(); 
	}

	
	public void testGFS() throws Exception {
		DefaultCacheManager dm = new DefaultCacheManager() ;
		
		ClusteringConfigurationBuilder meta_builder = new ConfigurationBuilder()
			.persistence().passivation(true).addSingleFileStore().location("./resource/temp/servera").purgeOnStartup(true).fetchPersistentState(true)
			.clustering().stateTransfer().timeout(30, TimeUnit.SECONDS)
			.eviction().maxEntries(10L).strategy(EvictionStrategy.LRU).expiration().lifespan(60, TimeUnit.SECONDS).clustering();

		ClusteringConfigurationBuilder data_builder = new ConfigurationBuilder()
			.persistence().passivation(true).addSingleFileStore().location("./resource/temp/servera").purgeOnStartup(true).fetchPersistentState(true)
			.clustering().stateTransfer().timeout(30, TimeUnit.SECONDS)
			.eviction().maxEntries(50L).strategy(EvictionStrategy.LRU).expiration().lifespan(60, TimeUnit.SECONDS).clustering();

		dm.defineConfiguration("metadata", meta_builder.build()) ;
		dm.defineConfiguration("data", data_builder.build()) ;
		
		dm.start(); 
		
		Cache<String, Metadata> metadata = dm.getCache("metadata") ;
		Cache<String, byte[]> data = dm.getCache("data") ;
		int defaultChunkSize = 16 * 1024;
		GridFilesystem gfs = new GridFilesystem(data, metadata, defaultChunkSize) ;

		metadata.addListener(new DataListener());
		
		File dir = new File("C:/download") ;
		for (File file : dir.listFiles()) {
			if (file.isDirectory()) continue ;
			OutputStream gout = gfs.getOutput(file.getName()) ;
			IOUtil.copyNClose(new FileInputStream(file), gout);
			Debug.line(file);
			Thread.sleep(200);
		}
		
		new InfinityThread().startNJoin(); 
		
		
		dm.stop();
	}
}



