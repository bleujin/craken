package net.ion.craken.loaders;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;

public class TestMongoStore extends TestCase {

	private ReadSession session;
	private RepositoryImpl repo;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		GlobalConfiguration globalConf = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("craken").addProperty("configurationFile", "resource/config/jgroups-udp.xml").build();
		Configuration defaultConf = new ConfigurationBuilder().clustering().cacheMode(CacheMode.DIST_SYNC).clustering().l1().enable().invocationBatching().enable(true)
			.clustering().hash().numOwners(2).unsafe()
			.eviction().maxEntries(100).loaders().preload(true).shared(true).addCacheLoader().cacheLoader(new NewMongoDBCacheStore()).addProperty("host", "61.250.201.78").addProperty("dbName", "craken").addProperty("dbCollection", "mycol")
			.purgeOnStartup(false).ignoreModifications(false).fetchPersistentState(true).async().enabled(false)
			.build();
		
		this.repo = RepositoryImpl.create(globalConf);
		repo.defineConfig("mywork.node", defaultConf) ;
		
		repo.start() ;
		
		this.session = repo.login("mywork") ;
	}
	
	public void tearDown() {
		repo.shutdown() ;
	}
	
	public void testWrite() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).addChild("address").property("city", "seoul") ;
				wsession.pathBy("/hero").property("name", "hero") ;
				return null;
			}
		}) ;
		
		session.pathBy("/", true).children().debugPrint() ;
	}
	
	public void testRead() throws Exception {
		
		final ReadNode readNode = session.pathBy("/bleujin");
		Debug.line(readNode.toMap()) ; 
		
		session.pathBy("/", true).children().debugPrint() ;
	}
	
}
