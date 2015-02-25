package net.ion.craken.node.crud;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;

import com.google.common.base.Predicate;

public class TestWalkRefChildren extends TestBaseCrud {

	public void testExcludeIfNotExist() throws Exception {
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").refTos("friend", "/hero", "/jin") ;
				wsession.pathBy("/hero").property("name", "hero").refTo("friend", "/jin") ;
				wsession.pathBy("/jin").property("name", "jin").refTo("friend", "/novision") ;
				wsession.pathBy("/novision").property("name", "novision").refTo("friend", "/unknown") ;
				return null;
			}
		}) ;
		
		session.pathBy("/bleujin").walkRefChildren("friend").includeSelf(true).debugPrint(); 
		
		assertEquals(6, session.pathBy("/bleujin").walkRefChildren("friend").includeSelf(true).count()) ; 
	}
	
	
	public void testSetLimitWhenInfinityLoop() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").refTos("friend", "/hero", "/jin") ;
				wsession.pathBy("/hero").property("name", "hero").refTo("friend", "/jin") ;
				wsession.pathBy("/jin").property("name", "jin").refTo("friend", "/bleujin") ;
				return null;
			}
		}) ;
		
		session.pathBy("/bleujin").walkRefChildren("friend").loopLimit(3).includeSelf(true).debugPrint(); 
		
		assertEquals(5, session.pathBy("/bleujin").walkRefChildren("friend").loopLimit(3).includeSelf(true).count()) ;
	}

	public void testStopWhenInfinityLoop() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").refTos("friend", "/hero", "/jin") ;
				wsession.pathBy("/hero").property("name", "hero").refTo("friend", "/jin") ;
				wsession.pathBy("/jin").property("name", "jin").refTo("friend", "/bleujin") ;
				return null;
			}
		}) ;
		
		session.pathBy("/bleujin").walkRefChildren("friend").loopLimit(3).includeSelf(true).debugPrint(); 
		
		assertEquals(5, session.pathBy("/bleujin").walkRefChildren("friend").loopLimit(3).includeSelf(true).count()) ;
	}

	
	public void testReverseRef() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/dept/dev").property("name", "dev").refTos("include", "/hero", "/jin") ;
				wsession.pathBy("/hero").property("name", "hero").refTo("friend", "/bleujin") ;
				wsession.pathBy("/jin").property("name", "jin").refTo("friend", "/bleujin") ;
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		
		session.root().childQuery("").refTo("friend", Fqn.fromString("/bleujin")).find().debugPrint(); 
		
	}
	
	public void testComplicate() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/a").property("name", "a").refTo("include", "/emp/b") ;
				wsession.pathBy("/emp/b").property("name", "b").refTos("include", "/emp/c", "/emp/d") ;
				wsession.pathBy("/emp/c").property("name", "c") ;
				wsession.pathBy("/emp/d").property("name", "d") ;
				wsession.pathBy("/emp/e").property("name", "e") ;
				wsession.pathBy("/emp/f").property("name", "f") ;
				return null;
			}
		}) ;
		
		// about c
		session.pathBy("/emp").children().filter(new Predicate<ReadNode>() {
			@Override
			public boolean apply(ReadNode read) {
				
				return false;
			}
		}).debugPrint();
	}
	
	
	
}
