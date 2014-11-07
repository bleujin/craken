package net.ion.craken.node.problem.speed;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.framework.db.DBController;
import net.ion.framework.db.manager.DBManager;
import net.ion.framework.db.manager.OracleCacheReleaseDBManager;
import net.ion.framework.db.servant.AfterTask;
import net.ion.framework.db.servant.IExtraServant;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;


public class TestBaseFromDB extends TestCase {

	protected DBController dc;
	private RepositoryImpl r;
	protected ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		DBManager dbm = new OracleCacheReleaseDBManager("jdbc:oracle:thin:@61.250.201.239:1521:qm10g", "ibr_15843", "ibr_15843", 10);
		this.dc = new DBController("testDc", dbm, new IExtraServant() {
			@Override
			public void support(AfterTask task) {
				Debug.line(task.getQueryable()) ;
			}
		}) ;
		dc.initSelf() ;
		
		GlobalConfiguration gconfig = GlobalConfigurationBuilder.defaultClusteredBuilder().transport().clusterName("craken").addProperty("configurationFile", "./resource/config/jgroups-udp.xml").build();
		this.r = RepositoryImpl.create() ;
		r.createWorkspace("test", WorkspaceConfigBuilder.directory("./resource/store/test")) ;
		
		r.start() ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		IOUtil.closeQuietly(dc) ;
		r.shutdown() ;
		super.tearDown();
	}
	
	protected ReadSession readSession(){
		return session ;
	}
}
