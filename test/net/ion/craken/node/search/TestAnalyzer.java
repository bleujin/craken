package net.ion.craken.node.search;


import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.util.Version;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import junit.framework.TestCase;

public class TestAnalyzer extends TestCase {

	public void testIndex() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest() ;
		ReadSession session = r.login("test") ;
		
		Central central = session.workspace().central();
		central.indexConfig().indexAnalyzer(new CJKAnalyzer(Version.LUCENE_35)) ;
		central.searchConfig().queryAnalyzer(new CJKAnalyzer(Version.LUCENE_35)) ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "뜨리거2") ;
				return null;
			}
		}) ;

		session.pathBy("/").childQuery("뜨리거").find().debugPrint(); 
		r.shutdown() ;
	}
	
	
	public void testAnalyerWithISarcher() throws Exception {
		Central central = CentralConfig.newRam().build() ;
		central.indexConfig().indexAnalyzer(new CJKAnalyzer(Version.LUCENE_35)) ;
		central.searchConfig().queryAnalyzer(new CJKAnalyzer(Version.LUCENE_35)) ;
		
		central.newIndexer().index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.newDocument("bleujin").text("name", "뜨리거2").update() ;
				return null;
			}
		}) ;
		
		central.newSearcher().createRequest("뜨리").find().debugPrint();
		central.close();
	}
}
