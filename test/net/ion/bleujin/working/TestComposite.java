package net.ion.bleujin.working;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.lucene.store.Directory;
import org.infinispan.Cache;
import org.infinispan.io.GridFile.Metadata;
import org.infinispan.io.GridFilesystem;
import org.infinispan.lucene.directory.BuildContext;
import org.infinispan.lucene.directory.DirectoryBuilder;
import org.infinispan.manager.DefaultCacheManager;

import net.ion.craken.node.crud.store.GridFileConfigBuilder;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJobs;
import junit.framework.TestCase;

public class TestComposite extends TestCase {

	public void testRun() throws Exception {
		String wsName = "test" ;
		DefaultCacheManager dcm = new DefaultCacheManager() ;
		Cache<String, Metadata> metaCache = dcm.getCache("meta");
		Cache<String, byte[]> dataCache = dcm.getCache("data");
		
		GridFilesystem gfs = new GridFilesystem(dataCache, metaCache) ;
		
		BuildContext bcontext = DirectoryBuilder.newDirectoryInstance(metaCache, dataCache, metaCache, wsName);
		bcontext.chunkSize(1024 * 1024);
		Directory directory = bcontext.create();
		Central central = CentralConfig.oldFromDir(directory).build();
		
		central.newIndexer().index(IndexJobs.create("/bleujin", 10)) ;
		
		central.newSearcher().createRequest("").find().debugPrint(); 
		
		
		OutputStream output = gfs.getOutput("/test.data") ;
		IOUtil.copyNClose(new StringInputStream("hello bleujin"), output);
		
		
		Debug.line(IOUtil.toStringWithClose(gfs.getInput("/test.data"))) ;
		
		central.newSearcher().createRequest("").find().debugPrint(); 
		
		File root = gfs.getFile("/") ;
		viewFile(root);
		
		dcm.stop(); 
		
	}
	
	
	public void testIndexAtSifs() throws Exception {
		
	}
	
	
	private void viewFile(File file){
		if (file.isDirectory()){
			for (File child : file.listFiles(new FileFilter() {
				@Override
				public boolean accept(File pathname) {
					Debug.debug(pathname);
					return false;
				}
			})) {
				viewFile(child) ;
			}
		} else {
			Debug.line(file);
		}
	}
}
