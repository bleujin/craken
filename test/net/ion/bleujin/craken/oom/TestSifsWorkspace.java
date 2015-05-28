package net.ion.bleujin.craken.oom;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.infinispan.configuration.cache.CacheMode;

import net.ion.bleujin.craken.TestCraken;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.CrakenWorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;

public class TestSifsWorkspace extends TestBaseWorkspace {

	private Craken craken;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.craken = Craken.create();
	}

	@Override
	protected void tearDown() throws Exception {
		craken.shutdown();
		super.tearDown();
	}

	public void testConfirmOOM() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/sifs"));

		craken.createWorkspace("sifs", CrakenWorkspaceConfigBuilder.sifsDir("./resource/sifs"));

		ReadSession session = craken.login("sifs");
		session.tran(makeJob(200000));
	}

	public void testIndexConfirm() throws Exception {
		craken.createWorkspace("sifs", CrakenWorkspaceConfigBuilder.sifsDir("./resource/sifs"));

		ReadSession session = craken.login("sifs");
		Debug.line(session.root().childQuery("", true).offset(1000).find().size());
	}

	public void testForceIndex() throws Exception {
		craken.createWorkspace("sifs", CrakenWorkspaceConfigBuilder.sifsDir("./resource/sifs"));

		ReadSession session = craken.login("sifs");
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().text("content");
				// wsession.root().index(new CJKAnalyzer(), true) ;
				return null;
			}

		});

	}

	public void testWriteBlob() throws Exception {
		craken.createWorkspace("sifs", CrakenWorkspaceConfigBuilder.sifsDir("./resource/sifs"));
		ReadSession session = craken.login("sifs");
		// session.tran(new TransactionJob<Void>(){
		// @Override
		// public Void handle(WriteSession wsession) throws Exception {
		// wsession.pathBy("/bleujin").blob("msg", new StringInputStream("hello world")) ;
		// return null;
		// }
		// }) ;

		InputStream input = session.pathBy("/bleujin").property("msg").asBlob().toInputStream();
		Debug.line(IOUtil.toStringWithClose(input));
	}

	
	public void testViewSearchIndex() throws Exception {
		
		craken.createWorkspace("sifs", CrakenWorkspaceConfigBuilder.sifsDir("./resource/sifs"));
		ReadSession session = craken.login("sifs");

		Debug.line(session.root().childQuery("", true).find().totalCount()) ;
	}
	
	public void testIndexDirect() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/sifs"));
		craken.createWorkspace("sifs", CrakenWorkspaceConfigBuilder.sifsDir("./resource/sifs"));
		ReadSession session = craken.login("sifs");
		
		Central central = session.workspace().central() ;

		Indexer indexer = central.newIndexer();
		final int maxcount = 50000;
		final AtomicInteger count = new AtomicInteger();

		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(final IndexSession isession) throws Exception {
				Files.walkFileTree(Paths.get(new File("C:/crawl/enha/wiki").toURI()), new SimpleFileVisitor<Path>() {
					private long start = System.currentTimeMillis();

					public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException {
						File file = path.toFile();
						try {
							if (file.isDirectory())
								return FileVisitResult.CONTINUE;
							int icount = count.incrementAndGet();

							if (icount >= maxcount)
								return FileVisitResult.TERMINATE;

							if ((icount % 300) == 0) {
								System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
								this.start = System.currentTimeMillis();
								isession.continueUnit();
							}

							String content = IOUtil.toStringWithClose(new FileInputStream(file), "UTF-8");
							String wpath = makePathString(path);
							isession.newDocument(wpath).text("content", content).update();

							return FileVisitResult.CONTINUE;
						} catch (Throwable e) {
							System.err.println(file);
							throw new IOException(e);
						}
					}
				});
				return null;
			}

			public String makePathString(Path path) {
				Iterator<Path> iter = path.iterator();
				List<String> result = ListUtil.newList();
				while (iter.hasNext()) {
					result.add(String.valueOf(iter.next()));
				}
				return "/" + StringUtil.join(result, "/");
			}

		});

	}
	
	public void testDirectSearch() throws Exception {
//			FileUtil.deleteDirectory(new File("./resource/sifs"));
		craken.createWorkspace("sifs", CrakenWorkspaceConfigBuilder.sifsDir("./resource/sifs").distMode(CacheMode.DIST_SYNC));
		ReadSession session = craken.login("sifs");
		Central central = session.workspace().central() ;
		Debug.line(central.newSearcher().createRequest("").find().totalCount()) ;
		new InfinityThread().startNJoin(); 
	}

}
