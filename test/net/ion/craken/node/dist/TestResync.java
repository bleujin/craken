package net.ion.craken.node.dist;

import java.io.File;
import java.io.InputStream;
import java.util.Map.Entry;

import org.apache.commons.lang.SystemUtils;
import org.infinispan.Cache;

import junit.framework.TestCase;
import net.ion.craken.io.GridBlob;
import net.ion.craken.io.GridFilesystem;
import net.ion.craken.io.Metadata;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TranLogManager;
import net.ion.craken.node.Workspace;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.IOUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.StringUtil;

public class TestResync extends TestCase{

	
	public void testRunMain() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/c1")) ;
		final RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c1")) ;
		
		r.start() ;

		ReadSession session = r.login("test");
		
		session.tranSync(TransactionJobs.dummy("/bleujin", 3)) ;
		session.tranSync(TransactionJobs.dummy("/hero", 3)) ;
		session.tranSync(TransactionJobs.dummy("/jin", 3)) ;

		new InfinityThread().startNJoin() ;
	}
	
	
	public void testResyncWhenEmpty() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/temp/c2")) ;
		final RepositoryImpl r = RepositoryImpl.create("dopple");
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c2")) ;
		
		r.start() ;
		
		Integer applied = r.getAttribute("TranLogManager.test", Integer.class);
		r.shutdown() ;
		
		assertEquals(new Integer(3), applied) ;
	}

	public void testResyncWhenBeforeData() throws Exception {
		final RepositoryImpl r = RepositoryImpl.create("dopple");
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c2")) ;
		
		r.start() ;
		
		Integer applied = r.getAttribute("TranLogManager.test", Integer.class);
		r.shutdown() ;
		assertEquals(new Integer(0), applied) ;
	}

	
	
	public void testView() throws Exception {
		final RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/temp/c1")) ;
		r.start() ;
		
		ReadSession session = r.login("test");
		final Workspace workspace = session.workspace();
		TranLogManager tmanager = workspace.tranLogManager();
		
		Cache<String, Metadata> meta = tmanager.logmeta();
		for (String key : meta.keySet()) {
			final GridBlob blob = tmanager.logContent().gridBlob(key, meta.get(key));
			Debug.line(key, IOUtil.toStringWithClose(blob.toInputStream())) ;
		}
		
		
	}
	
	
	
	
	
}
