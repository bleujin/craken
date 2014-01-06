package net.ion.craken.node.dist;

import java.io.File;
import java.util.List;
import java.util.Set;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.ISearcherWorkspaceConfig;
import net.ion.craken.node.AbstractWriteSession;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.dist.ServerStatus.ElectRecent;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.ChainMap;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.SetUtil;
import net.ion.nsearcher.common.IKeywordField;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;

public class TestRepositoryListener extends TestCase{
	

	
	public void xtestSave() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/save")) ;
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/save")) ;
		ReadSession session = r.login("test");
		session.tranSync( new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.tranId("12345") ;
				for (int i : ListUtil.rangeNum(10)) {
					wsession.root().addChild("/bleujin").addChild("" + i).property("name", "bleujin").property("dummy", i) ;
				}
				return null;
			}
		}) ;
		
		session.pathBy("/__transactions/12345").children().debugPrint() ;
		
		r.shutdown() ;
	}
	
	public void xtestRead() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", ISearcherWorkspaceConfig.create().location("./resource/save")) ;
		ReadSession session = r.login("test");
		
		Debug.line(r.lastSyncModified()) ;

		ReadNode tranNode = session.pathBy("/__transactions/12345");
		Debug.debug(tranNode.property("time").stringValue(), tranNode.property("config").stringValue()) ;
		for (ReadNode child : tranNode.children()) {
			Debug.line(child, child.property("path").stringValue(), child.property("touch").stringValue(), child.property("val").stringValue()) ;
		} 
		r.shutdown() ;
	}
	
	
	public void testLastModInfo() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create()) ;
		ReadSession session = r.login("test");
		session.tranSync( new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.tranId("12345") ;
				for (int i : ListUtil.rangeNum(10)) {
					wsession.root().addChild("/bleujin").addChild("" + i).property("name", "bleujin").property("dummy", i) ;
				}
				return null;
			}
		}) ;
		
		assertEquals(1, session.pathBy(Fqn.TRANSACTIONS).children().toList().size()) ;
		assertEquals(10, session.pathBy("/__transactions/12345").children().toList().size()) ;
		
		ReadNode tranNode = session.pathBy("/__transactions/12345") ;
		assertEquals(true, tranNode.property("time").longValue(0) > 0 && tranNode.property("time").longValue(0) <= System.currentTimeMillis()) ;
		

		for (ReadNode logNode : session.pathBy("/__transactions/12345").children()) {
			assertEquals(Touch.MODIFY.toString(), logNode.property("touch").stringValue()) ;
		}
		
		assertEquals(true, r.lastSyncModified().get("test") > 0 ) ;
		
		
		
		// tranBean
		
		List<ReadNode> recentTrans = session.logManager().recentTran(0);
		for (ReadNode tran : recentTrans) {
			for (ReadNode log : tran.children()){
				Debug.line(log.property("path").stringValue()) ;
			}
		}
		
		
		r.shutdown() ;
	}
	
	public void testCreateMyStatus() throws Exception {
		ServerStatus newbie   = new ServerStatus().lastTran(new ChainMap<String, Long>().put("aw", 1L).put("ay", 1L).toMap()).memeberName("newbie");
		ServerStatus awmaster = new ServerStatus().lastTran(new ChainMap<String, Long>().put("aw", 3L).put("ay", 2L).toMap()).memeberName("awmaster");
		ServerStatus aymaster = new ServerStatus().lastTran(new ChainMap<String, Long>().put("aw", 2L).put("ay", 4L).toMap()).memeberName("aymaster");
		final Set<ServerStatus> status = SetUtil.create(newbie, awmaster, aymaster);
		
		
		assertEquals(0, ElectRecent.elect(status, "newbie",   SetUtil.create("aw", "ay")).size()) ;
		assertEquals(1, ElectRecent.elect(status, "awmaster", SetUtil.create("aw", "ay")).size()) ;
		assertEquals(new Long(1), ElectRecent.elect(status, "awmaster", SetUtil.create("aw", "ay")).get("aw")) ;
		assertEquals(1, ElectRecent.elect(status, "aymaster",SetUtil.create("aw", "ay")).size()) ;
		assertEquals(new Long(1), ElectRecent.elect(status, "aymaster", SetUtil.create("aw", "ay")).get("ay")) ;

		Debug.line(newbie.toJsonString()) ;
	}
	
	
	public void testElect() throws Exception {
		ServerStatus newbie   = new ServerStatus().lastTran(new ChainMap<String, Long>().put("aw", 1L).put("ay", 1L).toMap()).memeberName("newbie");
		ServerStatus awmaster = new ServerStatus().lastTran(new ChainMap<String, Long>().put("aw", 3L).put("ay", 2L).toMap()).memeberName("awmaster");
		
	}
	
	
	public void testOnceWriteServer() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create().maxNodeEntry(5)) ;
		ReadSession session = r.login("test");
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 20 ; i++) {
					wsession.pathBy("/mod/" + i).property("index", i) ;
				}
				return null;
			}
		}); 
		
		new InfinityThread().startNJoin() ;
	}
	
	
	public void testReadOnlyServer() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create().maxNodeEntry(5)) ;
		ReadSession session = r.login("test");

		while(true){
			Thread.sleep(2000) ;
			if (session.exists("/mod")){
				Debug.line(session.pathBy("/mod").children().toList().size()) ;
			}
		}
//		new InfinityThread().startNJoin() ;
	}
	
	
	
	
	public void testMyStatus() throws Exception {
		ServerStatus status = new ServerStatus().started(100L).lastTran(MapUtil.create("dd", 100L)).memeberName("bleujin");
		
		ServerStatus read = ServerStatus.fromJson(status.toJsonString());
		assertEquals(true, read.equals(status)) ;
	}
	
	
	public void testOnEvent() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspaceForTest("test", ISearcherWorkspaceConfig.create()) ;
		ReadSession session = r.login("test");

		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.tranId("savedtranid") ;
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}) ;
		
		assertEquals(true, session.exists("/bleujin")) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").stringValue()) ;

		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").removeSelf() ;
				assertEquals(false, wsession.workspace().cache().containsKey(Fqn.fromString("/bleujin").dataKey())) ;
				return null;
			}
		}) ;
		
		assertEquals(false, session.exists("/bleujin")) ;
		
		
		
		final String addresss = r.dm().getAddress().toString() ;
		
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				
				((AbstractWriteSession)wsession).restoreOverwrite() ;
				wsession.pathBy("/__transactions/savedtranid").property("address", addresss);
				assertEquals(false, wsession.workspace().cache().containsKey(Fqn.fromString("/bleujin").dataKey())) ;
				
				return null;
			}
		}) ;
		// ... -_-
		session.workspace().cache().clear() ;
		
		
		
		
		assertEquals(1, session.workspace().central().newSearcher().createRequest(new TermQuery(new Term(IKeywordField.ISKey, "/bleujin"))).find().getDocument().size()) ;
		assertEquals(true, session.exists("/bleujin")) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").stringValue()) ;
		
		assertEquals(1, session.root().children().toList().size()) ;
		

		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").removeSelf() ;
				assertEquals(false, wsession.workspace().cache().containsKey(Fqn.fromString("/bleujin").dataKey())) ;
				return null;
			}
		}) ;
		
		assertEquals(0, session.workspace().central().newSearcher().createRequest(new TermQuery(new Term(IKeywordField.ISKey, "/bleujin"))).find().getDocument().size()) ;
		assertEquals(false, session.exists("/bleujin")) ;
		
		
		

//		session.pathBy("/bleujin").toRows("name, age").debugPrint() ;
//		session.root().children().debugPrint() ;
	
		
		
//		assertEquals(false, session.exists("/bleujin")) ;
		
		
		
		r.shutdown() ;
	}
}
