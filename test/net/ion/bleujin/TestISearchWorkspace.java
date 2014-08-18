package net.ion.bleujin;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;

import org.infinispan.manager.DefaultCacheManager;

public class TestISearchWorkspace extends TestCase{


	public void testLoad() throws Exception {
		RepositoryImpl r = RepositoryImpl.test(new DefaultCacheManager(), "niss");
		r.defineWorkspaceForTest("admin", ISearcherWorkspaceConfig.create().location(""));
		r.start();
		
		ReadSession session = r.login("admin") ;
		
		session.root().debugPrint(); 
		
		r.shutdown() ;
	}
	
	
}
