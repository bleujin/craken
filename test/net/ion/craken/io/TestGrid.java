package net.ion.craken.io;

import java.io.ByteArrayInputStream;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.manager.DefaultCacheManager;

public class TestGrid extends TestCase {

	private DefaultCacheManager dm;
	private Cache<String, Metadata> metaCache;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.dm = new DefaultCacheManager();
		dm.defineConfiguration("metadata", FastFileCacheStore.fileStoreConfig(CacheMode.LOCAL, "./resource/temp/metadata", 100)) ;
		dm.defineConfiguration("data", FastFileCacheStore.fileStoreConfig(CacheMode.LOCAL, "./resource/temp/data", 100)) ;
		this.metaCache = dm.getCache("metadata") ;
		Cache<String, byte[]> data = dm.getCache("data");
		
	}
	
	public void testRepeatRead() throws Exception {
		final Cache<String, byte[]> dcache = dm.getCache("data");
		GridFilesystem gfs = new GridFilesystem(dcache);
		
		final String path = "/hello";
		GridBlob blob = gfs.newGridBlob(path);
		IOUtil.copyNClose(new ByteArrayInputStream(new String("HELLO").getBytes()),  blob.outputStream());
		metaCache.put(path, blob.getMetadata()) ;
		
		
		for (int i = 0; i < 5; i++) {
			Metadata loadedMeta = metaCache.get(path);
			assertEquals("HELLO", IOUtil.toStringWithClose(gfs.gridBlob(path, loadedMeta).toInputStream())) ;
		}
	}
	
	public void testMultiFile() throws Exception {
		final Cache<String, byte[]> dcache = dm.getCache("data");
		GridFilesystem gfs = new GridFilesystem(dcache);

		final String path = "/hello";
		for (int i = 0; i < 5; i++) {
			GridBlob blob = gfs.newGridBlob(path + i);
			IOUtil.copyNClose(new ByteArrayInputStream(new String("HELLO " + i).getBytes()),  blob.outputStream());
			metaCache.put(path + i, blob.getMetadata()) ;
		}

		
		for (int i = 0; i < 5; i++) {
			final Metadata metadata = metaCache.get(path + i);
			GridBlob blob = gfs.gridBlob(path + i, metadata);
			Debug.line(metadata, IOUtil.toStringWithClose(blob.toInputStream())) ;
		}
		
	}
	
	
	
	
	
	
	
}
