package net.ion.craken.node.problem.speed;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;

public class TestSelectFilterSpeed extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
//		FileUtil.deleteDirectory(new File("./resource/insert")) ;
		
		this.r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/insert"));
		r.start();
		this.session = r.login("test");
	}

	@Override
	protected void tearDown() throws Exception {
		r.shutdown();
		super.tearDown();
	}

	public void testSelect() throws Exception {
		long start = System.currentTimeMillis() ;
		int totalCount = session.pathBy("/bleujin").childQuery("").query("section_name:clinical").find().totalCount() ;
		Debug.line(totalCount, System.currentTimeMillis() - start) ;
	}
	
}
