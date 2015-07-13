package net.ion.bleujin.craken.newdefine;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.ion.bleujin.craken.TestCraken;
import net.ion.craken.io.FileVisitor;
import net.ion.craken.io.Files;
import net.ion.craken.node.crud.tree.TreeCache;
import net.ion.craken.node.crud.tree.TreeCacheFactory;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;

import org.infinispan.Cache;
import org.infinispan.atomic.impl.AtomicHashMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.commands.tx.CommitCommand;
import org.infinispan.commands.write.DataWriteCommand;
import org.infinispan.commands.write.PutKeyValueCommand;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.impl.TxInvocationContext;
import org.infinispan.interceptors.base.BaseCustomInterceptor;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;

public class TestNodeValue extends TestCase {

	
	private DefaultCacheManager dcm;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.dcm = new DefaultCacheManager() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		dcm.stop(); 
		super.tearDown();
	}
	
	public void testTreeNode() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/store/working"));
		dcm.defineConfiguration("working", new ConfigurationBuilder().persistence().addSingleFileStore().location("./resource/store/working").invocationBatching().enable(true).build()) ;
		
		
		ExecutorService es = Executors.newFixedThreadPool(5) ;
		final Cache<PropertyId, PropertyValue> cache = dcm.getCache("working") ;
		cache.getAdvancedCache().addInterceptor(new BaseCustomInterceptor(){
			@Override
			 public Object visitCommitCommand(final TxInvocationContext ctx, CommitCommand command) throws Throwable {
				List<DataWriteCommand> list = ctx.getModifications() ;
				for (DataWriteCommand cmd : list) {
					switch(cmd.getCommandId()){
					case PutKeyValueCommand.COMMAND_ID :
						PutKeyValueCommand pcmd = (PutKeyValueCommand) cmd ;
						AtomicHashMap valMap = (AtomicHashMap) pcmd.getValue() ;
						Debug.line(pcmd.getKey(), valMap.keySet(), valMap.values());
						break ;
					default : 
						break;
					}
				}
				return invokeNextInterceptor(ctx, command) ;
			}
		}, 0);
		
		final TreeCache<PropertyId, PropertyValue> tcache = new TreeCacheFactory().createTreeCache(cache) ;

		final Writer writer = new OutputStreamWriter(System.out) ;
		
		Callable<Void> call1 = new Callable<Void>(){
			@Override
			public Void call() throws Exception {
				BatchContainer bcon = cache.getAdvancedCache().getBatchContainer() ;
				bcon.startBatch(true) ;
				try {
					tcache.put("/emp/bleujin", PropertyId.fromIdString("name"), PropertyValue.createPrimitive("bleujin")) ;
					tcache.put("/emp/bleujin", PropertyId.fromIdString("age"), PropertyValue.createPrimitive(20)) ;
					
					tcache.getNode("/emp/bleujin").getData().put(PropertyId.fromIdString("address"), PropertyValue.createPrimitive("seoul")) ;
					
				} finally {
					bcon.endBatch(true, true);
				}
				return null;
			}
		} ;
		Callable<Void> call2 = new Callable<Void>(){
			public Void call(){
//				Cache<PropertyId, PropertyValue> othercache = dcm.getCache("working") ;
//				TreeCache<PropertyId, PropertyValue> mycache = new TreeCacheFactory().createTreeCache(othercache) ;
				try {
					writer.write(tcache.getNode("/emp/bleujin").getData().toString()) ;
				} catch (IOException e) {
					e.printStackTrace();
				}
				return null ;
			}
		} ;
		
		Callable<Void> call3 = new Callable<Void>(){
			public Void call(){
				//TreeNode<PropertyId, PropertyValue> find = tcache.getNode("/emp/bleujin");
				return null ;
			}
		};
		
		es.submit(call1) ;
		es.submit(call2) ;
		es.submit(call3) ;
		
		Thread.sleep(500);
	}
	
	public void testReadAfter() throws Exception {
		dcm.defineConfiguration("working", new ConfigurationBuilder().persistence().addSingleFileStore().location("./resource/store/working").invocationBatching().enable(true).build()) ;

		Cache<PropertyId, PropertyValue> othercache = dcm.getCache("working") ;
		TreeCache<PropertyId, PropertyValue> mycache = new TreeCacheFactory().createTreeCache(othercache) ;
		Debug.line(mycache.getNode("/emp/bleujin").getData()) ;
	}
	
	
	
	
	
	public void testInsertSpeed() throws Exception {
		
		String indexPath = "./resource/store/index";
		String chunkPath = "./resource/store/chunk";
		
		FileUtil.deleteDirectory(new File(indexPath));
		FileUtil.deleteDirectory(new File(chunkPath));
		
		dcm.defineConfiguration("working", new ConfigurationBuilder()
			.persistence().invocationBatching().enable().transaction().transactionMode(TransactionMode.TRANSACTIONAL)
			.persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false)
			.preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(indexPath).dataLocation(chunkPath).async().disable()
			.eviction().maxEntries(1000)
			.clustering().build());
		
		final Cache<String, String> cache = dcm.getCache("working") ;
		final TreeCache<String, String> tcache = new TreeCacheFactory().createTreeCache(cache) ;

		final BatchContainer bcon = cache.getAdvancedCache().getBatchContainer() ;
		bcon.startBatch(true) ;
		
		Files.walkFileTree(new File("C:/crawl/enha/wiki"), new FileVisitor() {
			private long start = System.currentTimeMillis();
			private AtomicInteger count = new AtomicInteger() ;
			private int maxcount = 200000 ;

			public FileVisitResult visitFile(File file) throws IOException {
				try {
					if (file.isDirectory())
						return FileVisitResult.CONTINUE;
					int icount = count.incrementAndGet();
					
					if (icount >= maxcount) return FileVisitResult.TERMINATE ;
					
					if ((icount % 200) == 0) {
						System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
						this.start = System.currentTimeMillis();
						
						bcon.endBatch(true, true);
						bcon.startBatch(true) ;
					}

					String content = IOUtil.toStringWithClose(new FileInputStream(file), "UTF-8");
					String wpath = makePath(file) ;
					cache.put(wpath, content);

					return FileVisitResult.CONTINUE;
				} catch (Throwable e) {
					System.err.println(file);
					throw new IOException(e);
				}
			}
			
			private String makePath(File path) throws IOException{
//				return "/" + new ObjectId().toString() ;
				return TestCraken.makePathString(path) ;
			}
			
		});
		
		bcon.endBatch(true, true);
	}
	
	public void testRead() throws Exception {
		String indexPath = "./resource/store/index";
		String chunkPath = "./resource/store/chunk";
		
		dcm.defineConfiguration("working", new ConfigurationBuilder().persistence().invocationBatching().enable().persistence().passivation(false)
			.persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false)
			.preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation(indexPath).dataLocation(chunkPath).async().disable()
			.eviction().maxEntries(10)
			.clustering().build());
		
		final Cache<PropertyId, PropertyValue> cache = dcm.getCache("working") ;
		final TreeCache<PropertyId, PropertyValue> tcache = new TreeCacheFactory().createTreeCache(cache) ;
		
		Debug.line(tcache.getNode("/crawl/enha/wiki").getChildrenNames()) ;
	}
}
