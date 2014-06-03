package net.ion.ics6.core;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import junit.framework.TestCase;

public class TestRecursive extends TestCase {

//	wsession.pathBy("/afield_rels/mq_ev_pdate").property("upperid", "ROOT").property("lowerid", "mq_ev_pdate").property("orderno", 1);
//	wsession.pathBy("/afield_rels/lyn_editor").property("upperid", "ROOT").property("lowerid", "lyn_editor").property("orderno", 1);
//	wsession.pathBy("/afield_rels/sortaf").property("upperid", "ROOT").property("lowerid", "sortaf").property("orderno", 1);
//	wsession.pathBy("/afield_rels/lyn_date").property("upperid", "ROOT").property("lowerid", "lyn_date").property("orderno", 1);
//	wsession.pathBy("/afield_rels/lyn_image").property("upperid", "ROOT").property("lowerid", "lyn_image").property("orderno", 1);
//	wsession.pathBy("/afield_rels/ch_set").property("upperid", "ROOT").property("lowerid", "ch_set").property("orderno", 1);
//	wsession.pathBy("/afield_rels/ch_set/ch_string").property("upperid", "ch_set").property("lowerid", "ch_string").property("orderno", 1);
//	wsession.pathBy("/afield_rels/ch_set/ch_boolean").property("upperid", "ch_set").property("lowerid", "ch_boolean").property("orderno", 2);
//	wsession.pathBy("/afield_rels/ch_set/ch_file").property("upperid", "ch_set").property("lowerid", "ch_file").property("orderno", 3);
//	wsession.pathBy("/afield_rels/ch_set/ch_image").property("upperid", "ch_set").property("lowerid", "ch_image").property("orderno", 4);
//	wsession.pathBy("/afield_rels/ch_set/ch_summary").property("upperid", "ch_set").property("lowerid", "ch_summary").property("orderno", 5);
//	wsession.pathBy("/afield_rels/ch_summary").property("upperid", "ROOT").property("lowerid", "ch_summary").property("orderno", 1);
	
	public void testRecursive() throws Exception {
		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest() ;
		ReadSession session = r.login("test") ;
		
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/afield_rel")
					.child("ch_set")
						.child("ch_string").property("orderno", 1).parent()
						.child("ch_boolean").property("orderno", 2).parent() 
						.child("ch_file").property("orderno", 3).parent() 
						.child("ch_image").property("orderno", 4) ; 
				return null;
			}
		}) ;
		
		session.pathBy("/afield_rel/ch_set").walkChildren().toAdRows("orderno").debugPrint(); 
		
		
	}
	
}
