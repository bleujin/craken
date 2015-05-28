package net.ion.bleujin.craken;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.TreeNodeKey;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.store.CrakenWorkspaceConfigBuilder;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.parse.gson.JsonObject;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.HashFunction;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.RandomUtil;
import net.ion.framework.util.StringUtil;
import net.ion.radon.util.csv.CsvReader;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.atomic.impl.AtomicHashMap;
import org.infinispan.batch.BatchContainer;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.persistence.sifs.configuration.SoftIndexFileStoreConfigurationBuilder;

public class TestInsertSpeed extends TestCase {
	

	public void testSoftIndex() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/sifs"));

		Craken r = Craken.create();
		r.createWorkspace("sifs", CrakenWorkspaceConfigBuilder.sifsDir("./resource/sifs/index", "./resource/sifs/data").eviMaxSegment(30));
		ReadSession session = r.login("sifs");
		session.tran(new TransactionJob<Void>() {

			private AtomicInteger count = new AtomicInteger(0);
			private long start = System.currentTimeMillis();

			@Override
			public Void handle(final WriteSession wsession) throws Exception {
//				final Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> sifsCache = wsession.workspace().cache() ;
				
				Files.walkFileTree(Paths.get(new File("C:/crawl/enha/wiki").toURI()), new SimpleFileVisitor<Path>() {
					private long start = System.currentTimeMillis();

					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
						File file = path.toFile();
						try {
							if (file.isDirectory())
								return FileVisitResult.CONTINUE;
							int icount = count.incrementAndGet();
							
//							if (icount > 200000) return FileVisitResult.TERMINATE ;
							
							if ((icount % 300) == 0) {
								System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
								this.start = System.currentTimeMillis();
								
								wsession.continueUnit();
							}

							String content = IOUtil.toStringWithClose(new FileInputStream(file), "UTF-8");
							String wpath = makePathString(path) ;
							wsession.pathBy(wpath).property("content", content);

							return FileVisitResult.CONTINUE;
						} catch (Exception e) {
							System.err.println(file);
							throw new IOException(e);
						}
					}
				});
				
				Debug.line(System.currentTimeMillis() - start);
				return null;
			}
		});
		r.shutdown() ;
	}
	
	
	public void testDataInsert() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/drug"));

		Craken r = Craken.create();
		r.createWorkspace("drug", CrakenWorkspaceConfigBuilder.sifsDir("./resource/drug/index", "./resource/drug/data").eviMaxSegment(30).maxEntry(10000));
		ReadSession session = r.login("drug");
		long start = System.currentTimeMillis() ;
//		session.workspace().withFlag(Flag.IGNORE_RETURN_VALUES, Flag.FORCE_ASYNCHRONOUS) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().async(true) ;
				
				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
				
				CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
				reader.setFieldDelimiter('\t') ;
				String[] headers = reader.readLine();
				String[] line = reader.readLine() ;
				int max = 20000 ;
				String prefix = "/drug/" + RandomUtil.nextInt(50) + "/";
				long rangeTime = System.currentTimeMillis() ;
				while(line != null && line.length > 0 && max-- > 0 ){
					WriteNode wnode = wsession.pathBy(prefix + max);
					for (int ii = 0, last = headers.length; ii < last ; ii++) {
						if (line.length > ii) wnode.property(headers[ii], line[ii]) ;
					}
					
					line = reader.readLine() ;
					if ((max % 1000) == 0) {
						wsession.continueUnit() ;
						Debug.line("rangetime", System.currentTimeMillis() - rangeTime) ;
						rangeTime = System.currentTimeMillis();
					} 
				}
				reader.close() ;
				Debug.line("endJob") ;
				return null;			
			}
		}) ;
		
		Debug.line(System.currentTimeMillis() - start);

		r.shutdown() ;
	}
	
	public void testDataIntsert2() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/drug"));

		Craken r = Craken.create();
		r.createWorkspace("drug", CrakenWorkspaceConfigBuilder.sifsDir("./resource/drug/index", "./resource/drug/data").eviMaxSegment(30).maxEntry(10000));
		ReadSession session = r.login("drug");
		long start = System.currentTimeMillis() ;
//		session.workspace().withFlag(Flag.IGNORE_RETURN_VALUES, Flag.FORCE_ASYNCHRONOUS) ; // not applid
		
		final Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache = session.workspace().cache() ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().async(true) ;

				File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
				
				CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
				reader.setFieldDelimiter('\t') ;
				String[] headers = reader.readLine();
				String[] line = reader.readLine() ;
				int max = 20000 ;
				String prefix = "/drug/" + RandomUtil.nextInt(50) + "/";
				long rangeTime = System.currentTimeMillis() ;
				while(line != null && line.length > 0 && max-- > 0 ){
					TreeNodeKey tkey = TreeNodeKey.fromString(prefix + max) ;
					AtomicMap<PropertyId, PropertyValue> tvalue = new AtomicHashMap<PropertyId, PropertyValue>() ;
					for (int ii = 0, last = headers.length; ii < last ; ii++) {
						if (line.length > ii) tvalue.put(PropertyId.fromIdString(headers[ii]), PropertyValue.createPrimitive(line[ii])) ;
					}
					cache.put(tkey, tvalue) ;
					
					line = reader.readLine() ;
					if ((max % 1000) == 0) {
						wsession.continueUnit() ;
						Debug.line("rangetime", System.currentTimeMillis() - rangeTime) ;
						rangeTime = System.currentTimeMillis();
					} 
				}
				reader.close() ;
				Debug.line("endJob") ;
				return null;			
			}
		}) ;
		
		Debug.line(System.currentTimeMillis() - start);

		r.shutdown() ;
	}
	

	public void testDataInsert3() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/drug"));

		DefaultCacheManager dcm = new DefaultCacheManager("./resource/config/craken-cache-config.xml") ;
		dcm.defineConfiguration("drug", new ConfigurationBuilder()
			.persistence().passivation(false).addStore(SoftIndexFileStoreConfigurationBuilder.class).fetchPersistentState(false)
			.preload(true).shared(false).purgeOnStartup(false).ignoreModifications(false).indexLocation("./resource/drug/index")
			.dataLocation("./resource/drug/data").async().disable().flushLockTimeout(300000).shutdownTimeout(2000)
			.modificationQueueSize(100).threadPoolSize(10).eviction().maxEntries(10000) // alert : no expire
			.transaction().invocationBatching().enable()
			.clustering().build()) ;
		
		long start = System.currentTimeMillis() ;
//		session.workspace().withFlag(Flag.IGNORE_RETURN_VALUES, Flag.FORCE_ASYNCHRONOUS) ; // not applid
		
		Cache<String, String> cache = dcm.getCache("drug") ;
		cache = cache.getAdvancedCache().withFlags(Flag.IGNORE_RETURN_VALUES, Flag.FORCE_ASYNCHRONOUS) ; // not applid
		
		BatchContainer bcon = cache.getAdvancedCache().getBatchContainer() ;
		
		File file = new File("C:/temp/freebase-datadump-tsv/data/medicine/drug_label_section.tsv") ;
		
		CsvReader reader = new CsvReader(new BufferedReader(new FileReader(file)));
		reader.setFieldDelimiter('\t') ;
		String[] headers = reader.readLine();
		String[] line = reader.readLine() ;
		int max = 20000 ;
		String prefix = "/drug/" + RandomUtil.nextInt(50) + "/";
		long rangeTime = System.currentTimeMillis() ;
		bcon.startBatch() ;
		
		while(line != null && line.length > 0 && max-- > 0 ){
			JsonObject jvalue = new JsonObject() ;
			for (int ii = 0, last = headers.length; ii < last ; ii++) {
				if (line.length > ii) jvalue.put(headers[ii], line[ii]) ;
			}
			cache.put(prefix + max, jvalue.toString()) ;
			
			line = reader.readLine() ;
			if ((max % 1000) == 0) {
				bcon.endBatch(true);
				bcon.startBatch() ;
				Debug.line("rangetime", System.currentTimeMillis() - rangeTime) ;
				rangeTime = System.currentTimeMillis();
			} 
		}
		reader.close() ;
		bcon.endBatch(true);
		Debug.line("endJob") ;
		
		Debug.line(System.currentTimeMillis() - start);
	}
	
	public void xtestContfirm() throws Exception {
		Craken r = Craken.create();
		r.createWorkspace("drug", CrakenWorkspaceConfigBuilder.sifsDir("./resource/drug/index", "./resource/drug/data").eviMaxSegment(30));
		ReadSession session = r.login("drug");

		session.root().childQuery("", true).find().debugPrint();
		r.shutdown() ;
	}
	
	
	
	private String makePathString(Path path) {
		Iterator<Path> iter = path.iterator() ;
		List<String> result = ListUtil.newList() ;
		while(iter.hasNext()){
			result.add(iter.next().toString());
		}
		return "/" + StringUtil.join(result, "/") ;
	}

	
	public void xtestMakePath() throws Exception {
		File file = new File("C:/crawl/enha/wiki/김은아");
		Debug.line(makePathString(Paths.get(file.toURI())));
	}
	
	public void testRead() throws Exception {
		Craken r = Craken.create();
		r.createWorkspace("sifs", CrakenWorkspaceConfigBuilder.sifsDir("./resource/sifs/index", "./resource/sifs/data").eviMaxSegment(30).distMode(CacheMode.DIST_SYNC));
		ReadSession session = r.login("sifs");
		
		ReadNode node = session.pathBy("/crawl/enha/wiki/김은아") ;
		node.debugPrint();
		r.shutdown() ;
	}
	
	public void testPath() throws Exception {
//		List<Path> list = ListUtil.toList(Paths.get(new File("C:/crawl/enha/wiki/김은아").toURI()), Paths.get(new File("C:/crawl/enha/wiki/김은아(EBS 성우)").toURI()), Paths.get(new File("C:/crawl/enha/wiki/김은아(KBS 성우)").toURI())) ;
//		for (Path p : list) {
//			Debug.line(makePathString(p));
//		}
		
		final Map<String, Path> readPath = MapUtil.newMap() ;
		final AtomicInteger count = new AtomicInteger(0);
		Files.walkFileTree(Paths.get(new File("C:/crawl/enha/wiki").toURI()), new SimpleFileVisitor<Path>() {
			private long start = System.currentTimeMillis();

			public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
				int icount = count.incrementAndGet();
				if (icount > 100000) return FileVisitResult.TERMINATE ;
				
				String wpath = makePathString(path) ;
				if (readPath.containsKey(wpath)){
					Debug.line(wpath, readPath.get(wpath), path);
				}
				readPath.put(wpath, path) ;
				
				return FileVisitResult.CONTINUE ;
			}
		}) ;
	}
	
	
	public void testHashCode() throws Exception {
		String s1 = "김진기";
		String s2 = "김택기";
		
		Debug.line(s1.hashCode(), s2.hashCode(), HashFunction.BKDRHash(s1), HashFunction.BKDRHash(s2));
	}
	
}
