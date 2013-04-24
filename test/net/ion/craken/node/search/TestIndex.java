package net.ion.craken.node.search;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;
import net.ion.nsearcher.search.SearchResponse;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;

public class TestIndex extends TestCase {

	private RepositorySearch r;
	protected ReadSearchSession session;

	@Override
	protected void setUp() throws Exception {
		// GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("crakensearch").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		// this.r = RepositoryImpl.create(gconfig).forSearch() ;
		this.r = RepositoryImpl.testSingle().forSearch();
		this.session = r.testLogin("test");
	}

	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown();
		super.tearDown();
	}

	public void testConcurrency() throws Exception {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				try {
					r.testLogin("test").createQuery().parse("bleujin").find().debugPrint();
					r.testLogin("test").tran(new TransactionJob<Void>() {
						@Override
						public Void handle(WriteSession wsession) {
							wsession.root().addChild("/" + RandomUtil.nextInt(3)).property("name", new String[] { "jin", "bleujin", "hero" }[RandomUtil.nextInt(3)]);
							return null;
						}
					}).get();
				} catch (Throwable e) {
					e.printStackTrace();
				}
			}
		};
		ExecutorService exec = Executors.newFixedThreadPool(5);
		for (int i : ListUtil.rangeNum(100)) {
			exec.submit(task);
		}
		exec.awaitTermination(5, TimeUnit.SECONDS);
	}

	public void testIndex() throws Exception {
		Runnable task = new Runnable() {
			@Override
			public void run() {
				session.tranSync(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) {
						wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", RandomUtil.nextInt(50));
						wsession.root().addChild("/hero").property("name", "hero").property("age", 25);
						wsession.root().addChild("/jin").property("name", "jin").property("age", 30);

						return null;
					}
				});
			}
		};

		ExecutorService indexexec = Executors.newFixedThreadPool(3);
		for (int i : ListUtil.rangeNum(100)) {
			indexexec.submit(task);
		}

		for (int i = 0; i < 100; i++) {
			ReadSearchSession other = r.testLogin("test");
			SearchResponse response = other.createQuery().parse("bleujin").find();
			Debug.line(i, response.getDocument().size(), response.getDocument());
			Thread.sleep(10);
		}

		indexexec.awaitTermination(3, TimeUnit.SECONDS);
		assertEquals("test", session.wsName());

	}

}
