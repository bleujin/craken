package net.ion.bleujin.infinispan;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.io.GridFilesystem;
import org.infinispan.io.GridFile.Metadata;
import org.infinispan.manager.DefaultCacheManager;

public class TestGrid extends TestCase {

	
	private DefaultCacheManager dm;
	private GridFilesystem gfs;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.dm = new DefaultCacheManager();
		dm.defineConfiguration("metadata", FastFileCacheStore.fileStoreConfig(CacheMode.LOCAL, "./resource/temp/metadata", 100)) ;
		dm.defineConfiguration("data", FastFileCacheStore.fileStoreConfig(CacheMode.LOCAL, "./resource/temp/data", 100)) ;
		Cache<String, Metadata> metadata = dm.getCache("metadata") ;
		Cache<String, byte[]> data = dm.getCache("data");
		
		this.gfs = new GridFilesystem(data, metadata, 2000);
	}
	
	@Override
	protected void tearDown() throws Exception {
		dm.stop() ;
		super.tearDown();
	}
	
	
	public void testAppend() throws Exception {
		OutputStream output = gfs.getOutput("/file", false);
		InputStream input = new FileInputStream("./resource/jquery-1.10.2.min.js") ;
		IOUtil.copyNClose(input, output) ;
		
		InputStream finput = gfs.getInput("/file");
		Debug.line(IOUtil.toByteArray(finput).length) ;
		finput.close() ;

		
		output = gfs.getOutput("/file", true);
		input = new FileInputStream("./resource/jquery-1.10.2.min.js") ;
		IOUtil.copyNClose(input, output) ;

		finput = gfs.getInput("/file");
		Debug.line(IOUtil.toByteArray(finput).length) ;
		finput.close() ;
	}
}
