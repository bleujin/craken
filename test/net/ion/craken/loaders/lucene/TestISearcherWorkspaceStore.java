package net.ion.craken.loaders.lucene;

import java.io.File;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.problem.store.SampleResetJob;
import net.ion.craken.node.problem.store.SampleWriteJob;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.RandomUtil;


public class TestISearcherWorkspaceStore extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create();
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	// 30sec per 20k(create, about 700 over per sec)
	// 18sec per 20k(load, about 1k lower per sec)
	// 65 sec(after mod)
	public void testLuceneDirStore() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/local")) ;
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/local")) ;
		
		r.start() ;
		
		this.session = r.login("test") ;
		long start = System.currentTimeMillis() ;
		session.tran(new SampleWriteJob(10000)).get() ;
		
		Debug.line(System.currentTimeMillis() - start) ;
	}
	
	
	public void testResetDirStore() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/local")) ;
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/local")) ;
		
		r.start() ;
		
		this.session = r.login("test") ;
		long start = System.currentTimeMillis() ;
		session.tran(new SampleResetJob(10000)).get() ;
		
		Debug.line(System.currentTimeMillis() - start) ;
	}
	
	
	
	
	public void testFind() throws Exception {
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/local")) ;

		r.start() ;
		
		this.session = r.login("test") ;
		long start = System.currentTimeMillis() ;
		for (int i : ListUtil.rangeNum(20)) {
			Debug.line(session.pathBy("/" + RandomUtil.nextInt(200)).toMap()) ;
		}
		Debug.line(System.currentTimeMillis() - start) ;
		
	}

	
	
}
