package net.ion.bleujin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import junit.framework.TestCase;
import net.ion.craken.io.GridFile;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.io.GridFile.Metadata;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestGrid extends TestCase {

	private GridFilesystem gfs;
	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
		DefaultCacheManager cacheManager = r.dm();

		cacheManager.defineConfiguration("distributed", new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC)
					.hash().numOwners(2).rehashWait(120000).rehashRpcTimeout(600000).l1().enabled(true).lifespan(600000)
					.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location","./resource/store")
					.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());
		cacheManager.defineConfiguration("replicated", new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC)
					.sync().replTimeout(20000)
					.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location","./resource/store")
					.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build());

		Cache<String, byte[]> data = cacheManager.getCache("distributed");
		Cache<String, GridFile.Metadata> metadata = cacheManager.getCache("replicated");
		this.gfs = new GridFilesystem(data, metadata);
		r.start();
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	public void testFirst() throws Exception {

		// Create directories
		File file = gfs.getFile("/grid-movies");
		file.mkdirs(); // creates directories /tmp/testfile/stuff

		// List all files and directories under "/usr/local"
		file = gfs.getFile("/grid-movies");
		File[] files = file.listFiles();

		// Create a new file
		file = gfs.getFile("/grid-movies/README.txt");
		file.createNewFile();
	}

	public void testSave() throws Exception {
		File file = gfs.getFile("/grid-movies/README.txt");
		file.mkdirs();

		InputStream in = new FileInputStream("/tmp/wildlife.wmv");
		OutputStream out = gfs.getOutput("/grid-movies/wildlife.wmv");
		byte[] buffer = new byte[20000];
		int len;
		while ((len = in.read(buffer, 0, buffer.length)) != -1)
			out.write(buffer, 0, len);
		in.close();
		out.close();

	}

	public void testList() throws Exception {
		File file = gfs.getFile("/grid-movies");
		File[] files = file.listFiles();
		Debug.line(files);

	}
	
	public void testCopy() throws Exception {
		InputStream input = gfs.getInput("/grid-movies/wildlife.wmv");
		FileOutputStream output = new FileOutputStream("./resource/temp/wildlife.wmv");
		IOUtil.copyNClose(input, output);
	}

}
