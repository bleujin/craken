package net.ion.craken.node.crud;

import net.ion.craken.loaders.lucene.ISearcherCacheStoreConfig;
import net.ion.craken.node.ReadSession;

import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.util.Version;

public class TestWorkspace extends TestBaseCrud {

	public void testViewConfig() throws Exception {
		ISearcherCacheStoreConfig config = (ISearcherCacheStoreConfig)session.workspace().config();
		
		assertEquals(true, session.workspace().config() == super.config) ;
		assertEquals("./resource/local", config.location()) ;
	}
	
	public void testIndexConfig() throws Exception {
		ISearcherCacheStoreConfig newconfig = ISearcherCacheStoreConfig.create().location("./resource/newtest");
		final CJKAnalyzer analyzer = new CJKAnalyzer(Version.LUCENE_CURRENT);
		newconfig.centralConfig().indexConfigBuilder().indexAnalyzer(analyzer);
		
		r.defineWorkspaceForTest("newwork", newconfig) ;
		ReadSession newSession = r.login("newwork");
		
		assertEquals(true, newSession.workspace().config() == newconfig) ;
		
		assertEquals(true, newSession.workspace().central().indexConfig().indexAnalyzer() == analyzer);
	}
}
