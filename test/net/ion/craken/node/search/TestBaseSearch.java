package net.ion.craken.node.search;

import net.ion.craken.node.crud.RepositoryImpl;
import junit.framework.TestCase;

public class TestBaseSearch extends TestCase {

	protected RepositorySearch r;
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
	
}
