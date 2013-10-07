package net.ion.craken.io;

import java.io.File;
import java.util.Set;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

import com.amazonaws.util.StringInputStream;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.TransactionLog;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.tree.PropertyId;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.nsearcher.common.IKeywordField;

public class TestLogLob extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		FileUtil.deleteDirectory(new File(CentralCacheStoreConfig.create().location())) ;
		this.r = RepositoryImpl.create() ;
		r.defineWorkspace("test", CentralCacheStoreConfig.create().maxNodeEntry(5)) ;
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
