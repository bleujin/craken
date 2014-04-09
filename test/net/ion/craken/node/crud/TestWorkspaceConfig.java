package net.ion.craken.node.crud;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.framework.util.Debug;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

public class TestWorkspaceConfig extends TestCase {

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
		r.defineWorkspace("test", ISearcherWorkspaceConfig.createDefault()) ;
		ReadSession session = r.login("test");
		
		ISearcherWorkspaceConfig config = (ISearcherWorkspaceConfig)session.workspace().config();
		
		assertEquals(true, session.workspace().config() == config) ;
		assertEquals("./resource/temp/isearch", config.location()) ;
		r.shutdown() ;
	}
	
	public void xtestIndexConfig() throws Exception {
		ISearcherWorkspaceConfig newconfig = ISearcherWorkspaceConfig.create();
		final StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
		newconfig.centralConfig().indexConfigBuilder().indexAnalyzer(analyzer);
		r.defineWorkspace("newwork", newconfig) ;
		
		r.start() ;
		ReadSession newSession = r.login("newwork");
		Debug.line('o', newconfig.hashCode(),  newconfig) ;
		assertEquals(true, newSession.workspace().config() == newconfig) ;
		assertEquals(true, newSession.workspace().central().indexConfig().indexAnalyzer() == analyzer);
	}
}
