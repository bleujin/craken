package net.ion.bleujin;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.WildcardQuery;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.config.CentralConfig;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;
import net.ion.nsearcher.search.Searcher;

public class TestHanja extends TestBaseCrud {

	public void testHanja() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.iwconfig().keyword("name") ;
				wsession.pathBy("/emp/bleujin").property("name", "三星電氣");
				return null;
			}
		});

		WildcardQuery query = new WildcardQuery(new Term("name", "三星*")) ;
		session.root().childQuery("name:三星*", true).find().debugPrint();
		
//		Debug.line(session.pathBy("/emp/bleujin").property("name").asString()) ;
		
	}
	
	public void testInCentral() throws Exception {
		Central cen = CentralConfig.newRam().build() ;
		Indexer indexer = cen.newIndexer() ;
		indexer.index(new IndexJob<Void>() {
			@Override
			public Void handle(IndexSession isession) throws Exception {
				isession.newDocument().text("name", "三星電氣").insert() ;
				return null;
			}
		}) ;
		
		WildcardQuery query = new WildcardQuery(new Term("name", "三星*")) ;
		cen.newSearcher().createRequest(query).find().debugPrint(); 
		cen.destroySelf();
	}

	public void testHanja2() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("name", "삼성전기");
				return null;
			}
		});

		session.root().childQuery("name:삼성*", true).find().debugPrint();
	}

}
