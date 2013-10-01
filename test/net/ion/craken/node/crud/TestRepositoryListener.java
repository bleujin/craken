package net.ion.craken.node.crud;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.commons.fileupload.FileUpload;
import org.apache.lucene.analysis.kr.utils.StringUtil;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.QueryWrapperFilter;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;

import com.sun.xml.internal.bind.v2.runtime.reflect.ListTransducedAccessorImpl;

import junit.framework.TestCase;
import net.ion.craken.loaders.lucene.CentralCacheStoreConfig;
import net.ion.craken.loaders.lucene.DocEntry;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.craken.node.crud.ServerStatus.ElectRecent;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.ChainMap;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;
import net.ion.framework.util.InfinityThread;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.nsearcher.common.IKeywordField;
import net.ion.nsearcher.common.ReadDocument;
import net.ion.nsearcher.search.SearchRequest;

public class TestRepositoryListener extends TestCase{

	
	public void testQuery() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspaceForTest("test", CentralCacheStoreConfig.create()) ;
		ReadSession session = r.login("test");

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		TransactionBean bean = session.pathBy("/__transactions").childQuery("").descending("time").findOne().toBean(TransactionBean.class);
		assertEquals(false, bean.iwconfig().isIgnoreBodyField()) ;
		Debug.line(bean) ;
		r.shutdown() ;
	}
	
	
	public void xtestSave() throws Exception {
		FileUtil.deleteDirectory(new File("./resource/save")) ;
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/save")) ;
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
		r.defineWorkspace("test", CentralCacheStoreConfig.create().location("./resource/save")) ;
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
		r.defineWorkspaceForTest("test", CentralCacheStoreConfig.create()) ;
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
		r.defineWorkspaceForTest("test", CentralCacheStoreConfig.create().maxNodeEntry(5)) ;
		ReadSession session = r.login("test");
		
		session.tranSync(TransactionJobs.dummy("/", 10)) ;
		
		new InfinityThread().startNJoin() ;
	}
	
	
	public void testReadOnlyServer() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspaceForTest("test", CentralCacheStoreConfig.create().maxNodeEntry(5)) ;
		ReadSession session = r.login("test");

		while(true){
			if (session.exists("/")){
				Debug.line(session.root().children().toList().size()) ;
			}
			Thread.sleep(1000) ;
		}
	}
	
	
	
	
	public void testMyStatus() throws Exception {
		ServerStatus status = new ServerStatus().started(100L).lastTran(MapUtil.create("dd", 100L)).memeberName("bleujin");
		
		ServerStatus read = ServerStatus.fromJson(status.toJsonString());
		assertEquals(true, read.equals(status)) ;
	}
}
