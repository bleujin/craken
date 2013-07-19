package net.ion.bleujin.infinispan;

import java.util.List;

import junit.framework.TestCase;
import net.ion.craken.loaders.FastFileCacheStore;
import net.ion.craken.loaders.lucene.CentralCacheStore;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.RandomUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;

public class TestCentralCacheStore extends TestCase {


	public void testRepository() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/ff5"));

		ReadSession session = r.login("test");
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 100; i++) {
					wsession.pathBy("/bleujin/" + i).property("index", i);
				}
				return null;
			}
		});

		r.shutdown();
	}

	public void testRead() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/ff5"));

		ReadSession session = r.login("test");
		session.pathBy("/bleujin").children().debugPrint();

		r.shutdown();

	}

	public void testRunning() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/ff5"));

		ReadSession session = r.login("test");
		while (true) {
			List<ReadNode> nodes = session.pathBy("/bleujin").children().toList();
			if (nodes.size() > 0) {
				Debug.line(nodes.size());
				session.tranSync(new TransactionJob<Void>() {
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						final int nextInt = RandomUtil.nextInt(1000);
						wsession.pathBy("/bleujin/" + nextInt).property("name", "bleujin").property("index", nextInt) ;
						return null;
					}
				}) ;
				Thread.sleep(7000);
			}
		}

	}

}
