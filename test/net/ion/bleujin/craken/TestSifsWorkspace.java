package net.ion.bleujin.craken;

import java.io.InputStream;
import java.util.Map;

import junit.framework.TestCase;
import net.ion.craken.listener.CDDHandler;
import net.ion.craken.listener.CDDModifiedEvent;
import net.ion.craken.listener.CDDRemovedEvent;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.store.WorkspaceConfigBuilder;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.util.StringInputStream;
import net.ion.framework.util.Debug;
import net.ion.framework.util.IOUtil;

import org.infinispan.configuration.cache.CacheMode;

public class TestSifsWorkspace extends TestCase {

	private Craken craken;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();

//		this.craken = Craken.inmemoryCreateWithTest() ;
//		FileUtil.deleteDirectory(new File("./resource/store/sifs"));
		this.craken = Craken.local();
		craken.createWorkspace("test", WorkspaceConfigBuilder.sifsDir("./resource/store/sifs").distMode(CacheMode.LOCAL)) ;
		this.session = craken.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		craken.shutdown() ;
		super.tearDown();
	}
	
	
	public void testPath() throws Exception {
		ReadSession session = craken.login("test") ;
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/a/b").property("greeting", "hi") ;
				return null;
			}
		}) ;
		
		session.root().children().debugPrint();
		Debug.line();
		session.root().walkChildren().debugPrint();
//		GridWorkspace gworkspace = (GridWorkspace)session.workspace() ;
//		File root = gworkspace.gfs().getFile("/") ;
//		Debug.line(root.listFiles()) ;
		
	}
	
	public void testFirst() throws Exception {
		session.tran(TransactionJobs.HelloBleujin) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").asString()) ;
		session.root().walkChildren().debugPrint();
	}
	
	public void testWriteBlob() throws Exception {
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").blob("msg", new StringInputStream("hello world")) ;
				return null;
			}
		}) ;
		
		InputStream input = session.pathBy("/bleujin").property("msg").asBlob().toInputStream() ;
		Debug.line(IOUtil.toStringWithClose(input));
	}
	
	public void xtestRead() throws Exception {
		session.root().walkChildren().debugPrint();
		
		InputStream input = session.pathBy("/bleujin").property("msg").asBlob().toInputStream() ;
		Debug.line(IOUtil.toStringWithClose(input));
	}
	
	public void xtestChildren() throws Exception {
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		session.pathBy("/emp").children().debugPrint(); 
		session.tran(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/hero").property("name", "hero") ;
				return null;
			}
		}) ;
		session.pathBy("/emp").children().debugPrint(); 
	}
	
	public void xtestChildren2() throws Exception {
		session.pathBy("/emp").children().debugPrint(); 
	}

	public void testCreateAncestor() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/hero/jin/dummy1").property("name", "dummy1");
				wsession.pathBy("/hero/jin/dummy2").property("name", "dummy2");
				wsession.pathBy("/hero/jin").removeChildren() ;
				return null;
			}
		}) ;
	}
	
	
	public void testQuery() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
//				wsession.iwconfig().ignoreIndex() ;
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				wsession.pathBy("/bleujin").append("age", 20, 30) ;
				wsession.pathBy("/hero/jin/dummy1").property("name", "dummy1");
				wsession.pathBy("/hero/jin/dummy2").property("name", "dummy2");
				wsession.pathBy("/hero/jin").removeChildren() ;
//				wsession.pathBy("/hero").removeSelf() ;
				return null;
			}
		}) ;
		session.root().childQuery("age:30", true).find().debugPrint(); // removed dummy1, dummy2 
	}
	
	public void testQueryAfter() throws Exception {
//		session.pathBy("/bleujin").debugPrint();
//		session.tran(new TransactionJob<Void>(){
//			@Override
//			public Void handle(WriteSession wsession) throws Exception {
//				wsession.root().reindex(true) ;
//				return null;
//			}
//		}) ;
		session.root().childQuery("age:30", true).find().debugPrint(); 
	}
	
	
	public void testCDDM() throws Exception {
		session.workspace().cddm().add(new CDDHandler() {
			@Override
			public String pathPattern() {
				return "/{name}";
			}
			
			@Override
			public TransactionJob<Void> modified(Map<String, String> resolveMap, CDDModifiedEvent event) {
				Debug.line(event.getValue().values());

				final String name = resolveMap.get("name") ;
				return new TransactionJob<Void>(){
					@Override
					public Void handle(WriteSession wsession) throws Exception {
						wsession.pathBy("/" + name + "/cdd").property("modified", true) ;
						return null;
					}
				};
			}
			
			@Override
			public TransactionJob<Void> deleted(Map<String, String> resolveMap, CDDRemovedEvent event) {
				return null;
			}
		});
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals(true, session.pathBy("/bleujin/cdd").property("modified").asBoolean().booleanValue()) ;
		
	}
	
	
}
