package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.loaders.lucene.OldCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.framework.util.Debug;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.ReusableAnalyzerBase;
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

public class TestWorkspace extends TestCase {

	private RepositoryImpl r;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.create() ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testViewConfig() throws Exception {
		r.defineWorkspace("test", CentralCacheStoreConfig.createDefault()) ;
		ReadSession session = r.login("test");
		
		CentralCacheStoreConfig config = (CentralCacheStoreConfig)session.workspace().config();
		
		assertEquals(true, session.workspace().config() == config) ;
		assertEquals("./resource/local", config.location()) ;
		r.shutdown() ;
	}
	
	public void xtestIndexConfig() throws Exception {
		CentralCacheStoreConfig newconfig = CentralCacheStoreConfig.create();
		final ReusableAnalyzerBase analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		newconfig.centralConfig().indexConfigBuilder().indexAnalyzer(analyzer);
		r.defineWorkspace("newwork", newconfig) ;
		
		r.start() ;
		ReadSession newSession = r.login("newwork");
		Debug.line('o', newconfig.hashCode(),  newconfig) ;
		assertEquals(true, newSession.workspace().config() == newconfig) ;
		assertEquals(true, newSession.workspace().central().indexConfig().indexAnalyzer() == analyzer);
	}
}
