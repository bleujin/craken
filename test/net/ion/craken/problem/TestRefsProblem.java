package net.ion.craken.problem;

import java.io.File;

import javax.naming.directory.InvalidSearchControlsException;

import junit.framework.TestCase;
import net.ion.craken.loaders.EntryKey;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WorkspaceConfigBuilder;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.common.SearchConstant;
import net.ion.nsearcher.search.Searcher;

public class TestRefsProblem extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

//		FileUtil.deleteDirectory(new File("./resource/temp/test"));
		
		this.r = RepositoryImpl.create() ;
		r.createWorkspace("test", WorkspaceConfigBuilder.directory("./resource/temp/test")) ;
		this.session = r.login("test") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	
	public void testRefs() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/dept/1") ;
				wsession.pathBy("/dept/2") ;
				wsession.pathBy("/dept/3") ;
				
				wsession.pathBy("/emp/bleujin").refTos("dept", "/dept/1") ;
				wsession.pathBy("/emp/bleujin").refTos("dept", "/dept/2") ;
				wsession.pathBy("/emp/bleujin").refTos("dept", "/dept/3") ;
				
				wsession.pathBy("/emp/bleujin").append("age", 20, 30, 40) ;
				return null;
			}
		}).get() ;
		
		session.pathBy("/emp/bleujin").walkRefChildren("dept").debugPrint(); 
	}
	
	public void testView() throws Exception {
		Searcher searcher = session.workspace().central().newSearcher() ;
		
		ReadDocument doc = searcher.createRequestByKey("/emp/bleujin").find().first() ;
		Debug.line(doc.asString(EntryKey.VALUE));
	}
	
	
	public void testRead() throws Exception {
		session.pathBy("/emp/bleujin").walkRefChildren("dept").debugPrint(); 
		Debug.line(session.pathBy("/emp/bleujin").property("age").asSet()) ;
	}

}
