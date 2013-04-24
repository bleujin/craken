package net.ion.craken.node.search;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.Repository;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestFirst extends TestCase {

	private RepositorySearch r ;
	protected ReadSearchSession session;

	@Override
	protected void setUp() throws Exception {
//		GlobalConfiguration globalConfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("crakensearch").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		this.r = RepositoryImpl.testSingle().forSearch() ;
		this.session = r.testLogin("test") ;
		
		Debug.line(session.root().id()) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		this.r.shutdown() ;
		super.tearDown();
	}
	
	public void testRoot() throws Exception {
		ReadNode root = session.root();
		assertEquals(true, root.property("__id") != null) ;
	}
	
	public void testListener() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.root().addChild("/bleujin").property("name", "bleujin");
				return null;
			}
		}).get() ;
	}
	
	public void testWhenInsert() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/1/2/3/4") ;
				
				wsession.root().addChild("/bleujin").property("name", "bleujin").property("age", 20) ;
				wsession.root().addChild("/hero").property("name", "hero").property("age", 20) ;
				wsession.root().addChild("/jin").property("name", "jin").property("age", 20) ;
				
				return null;
			}
		}).get() ;
		
		
//		new InfinityThread().startNJoin() ;
	}
	
	
	
}
