package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.ListUtil;

public class TestRemoveChildren extends TestBaseCrud {

	public void testRemove() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").stringValue()) ;
		assertEquals(true, session.exists("/bleujin")) ;
		
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").removeSelf() ;
				return null;
			}
		}) ;
		assertEquals(false, session.exists("/bleujin")) ;
	}
	
	public void testRemoveTwice() throws Exception {
		
		for (int i = 0; i < 3; i++) {
			session.tranSync(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/bleujin").property("name", "bleujin") ;
					return null;
				}
			}) ;
			assertEquals("bleujin", session.pathBy("/bleujin").property("name").stringValue()) ;
			assertEquals(true, session.exists("/bleujin")) ;
			
			session.tranSync(new TransactionJob<Void>(){
				@Override
				public Void handle(WriteSession wsession) throws Exception {
					wsession.pathBy("/bleujin").removeSelf() ;
					assertEquals(false, wsession.workspace().cache().containsKey(Fqn.fromString("/bleujin").dataKey())) ;

					return null;
				}
			}) ;
			assertEquals(false, session.exists("/bleujin")) ;
		}
	}
	
	
	public void testRemoveAfter() throws Exception {
		assertEquals(true, session.exists("/")) ;
		assertEquals(true, session.exists("/")) ;

//		session.tranSync(new SampleWriteJob(20));
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 20; i++) {
					wsession.createBy("/" + i).property("idx", i) ;
				}
				return null;
			}
		}) ;
		
	 	assertEquals(20, session.root().children().toList().size()) ;
	 	assertEquals(true, session.exists("/")) ;
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root().removeChildren() ;
				return null;
			}
		}) ;
		
	 	assertEquals(0, session.root().children().toList().size()) ;
	 	assertEquals(true, session.exists("/")) ;

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i : ListUtil.rangeNum(20)) {
					final WriteNode wnode = wsession.pathBy("/bleujin/" + i);
					wnode.property("key", "val") ;
				}
				return null;
			}
		}) ;

		assertEquals(true, session.exists("/")) ;
		
	 	assertEquals(20, session.pathBy("/bleujin").children().toList().size()) ;
		session.tranSync(new TransactionJob<Void>() {
			public Void handle(WriteSession wsession) throws Exception {
				wsession.root().removeChildren() ;
				return null;
			}
		}) ;
	 	assertEquals(0, session.root().children().toList().size()) ;
	}
	
	
}
