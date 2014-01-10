package net.ion.craken.node.crud;

import java.io.File;
import java.util.List;

import org.infinispan.manager.DefaultCacheManager;

import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.framework.util.FileUtil;
import junit.framework.TestCase;

public class TestTranManager extends TestCase {

	
	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		
		
//		FileUtil.deleteDirectory(new File("./resource/temp/c1")) ;
//		this.r = RepositoryImpl.create() ;
//		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c1")) ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testInmemory() throws Exception {
		DefaultCacheManager dm = r.dm();
		
		System.out.print(' ') ;
	}
	
	public void testRegister() throws Exception {
		r.start() ;
		
		ReadSession session = r.login("test");
		List<String> names = session.workspace().tranLogManager().memberNames();
		
		assertEquals(1, names.size()) ;
		assertEquals("emanon", names.get(0)) ;
	}
}
