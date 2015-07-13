package net.ion.bleujin.crawl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.OldFileConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestIndexCrawlData extends TestCase {

	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GlobalConfiguration gconfig = new GlobalConfigurationBuilder()
		.transport().defaultTransport()
			.clusterName("storetest")
			.nodeName("external")
			.addProperty("configurationFile", "./resource/config/jgroups-udp.xml")
			.transport().addProperty("maxThreads", "100").addProperty("threadNamePrefix", "mytransport-thread")
		.build();
		DefaultCacheManager dcm = new DefaultCacheManager(gconfig) ;
		
		Craken r = Craken.create(dcm, "bleujin");
		r.createWorkspace("enha", OldFileConfigBuilder.directory("./resource/enha").distMode(CacheMode.REPL_SYNC));
		this.session = r.login("enha");
	}

	@Override
	protected void tearDown() throws Exception {
		session.workspace().repository().shutdown();
		super.tearDown();
	}

	public void testRespository() throws Exception {
		session.tran(new TransactionJob<Void>() {

			private AtomicInteger count = new AtomicInteger(0);
			private long start = System.currentTimeMillis();

			@Override
			public Void handle(WriteSession wsession) throws Exception {
				File homeDir = new File("C:/crawl/enha/wiki");
				saveProperty(wsession, homeDir, "");

				return null;
			}

			private void saveProperty(WriteSession wsession, File file, String path) throws IOException {
				int icount = count.incrementAndGet();
				if (icount >= 200) return;

				if (file.isDirectory()) {
					for (File sfile : file.listFiles()) {
						saveProperty(wsession, sfile, path + "/" + sfile.getName());
					}
				} else {
					String content = IOUtil.toStringWithClose(new FileInputStream(file), "UTF-8");
					wsession.pathBy(path).property("content", content);

					if ((icount % 50) == 0) {
						System.out.println(count.get() + " committed. elapsed time for unit : " + (System.currentTimeMillis() - start));
						this.start = System.currentTimeMillis();
						wsession.continueUnit();
					}

				}
			}
		});

		// session.pathBy(parentName).children().debugPrint();
	}

	public void testCount() throws Exception {
		Debug.line("loaded");
		int count = session.root().childQuery("").find().totalCount() ;
		Debug.line(count);
		new InfinityThread().startNJoin();
	}
	
	
}