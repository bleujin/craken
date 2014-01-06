package net.ion.craken.io;

import java.io.File;
import java.util.Set;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.TransactionLog;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.tree.PropertyId;
import net.ion.framework.util.FileUtil;
import net.ion.nsearcher.common.IKeywordField;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

public class TestLogLob extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		FileUtil.deleteDirectory(new File(ISearcherWorkspaceConfig.create().location())) ;
		this.r = RepositoryImpl.create() ;
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().maxNodeEntry(5)) ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}

	public void testFindLob() throws Exception {
		testWrite() ;
		
		session.workspace().central().newSearcher().createRequest(new TermQuery(new Term(IKeywordField.ISKey, "/__transactions/abcd"))).find().debugPrint() ;
		final Set<PropertyId> propKeys = session.pathBy("/__transactions/abcd").keys();
		assertEquals(3, propKeys.size()) ;
		
		assertEquals(true, propKeys.contains(TransactionLog.PropId.CONFIG)) ;
		assertEquals(true, propKeys.contains(TransactionLog.PropId.TIME)) ;
		assertEquals(true, propKeys.contains(PropertyId.normal("tran"))) ;
	}


	
	
	public void testWrite() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.tranId("abcd") ;
				for (int i = 0; i < 30; i++) {
					wsession.resetBy("/bleujin/" + i).property("name", "bleujin") ;
				}
				return null;
			}
		}) ;
	}
	
	
}
