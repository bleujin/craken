package net.ion.craken.io;

import java.io.File;
import java.io.FileInputStream;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.Debug;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;

public class TestNodeIo extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
		r.defineConfig("test.node",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC).invocationBatching().enable().clustering()
				.sync().replTimeout(20000)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location","./resource/store/test")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;

		r.defineConfig("test.blobdata",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC)
				.sync().replTimeout(20000)
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location","./resource/store/test")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		
		r.defineConfig("test.blobmeta",  new ConfigurationBuilder().clustering().cacheMode(CacheMode.REPL_SYNC)
				.sync().replTimeout(20000)
				
				.loaders().preload(true).shared(false).passivation(false).addCacheLoader().cacheLoader(new FastFileCacheStore()).addProperty("location","./resource/store/test")
				.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false).build()) ;
		this.session = r.testLogin("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testWrite() throws Exception {
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				WriteNode bleujin = wsession.pathBy("/bleujin").blob("config", new FileInputStream("./resource/config/server-simple.xml"));
				
				Debug.line(bleujin.property("config").asBlob().toFile()) ;
				return null;
			}
		}) ;
		
		Debug.line(session.pathBy("/bleujin").property("config").asBlob().toFile()) ;
	}
	
	public void testRead() throws Exception {
		final PropertyValue property = session.pathBy("/bleujin").property("config");
		
		Debug.line(property.stringValue()) ;
		
		final BlobValue blob = property.asBlob();
		final File file = blob.toFile();
		Debug.line(file, file.getParentFile(), file.isDirectory()) ;
//		Debug.debug(IOUtil.toString(blob.toInputStream())) ;
	}
	
	
	
	
}
