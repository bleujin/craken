package net.ion.craken.node.crud;

import java.util.Iterator;

import net.ion.craken.Craken;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TraversalStrategy;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

public class TestRelation extends TestBaseCrud {

	
	public void testCreateRelation() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").refTos("friend", "/hero").refTos("friend", "/jin") ;
				wsession.pathBy("/hero").property("name", "hero") ;
				wsession.pathBy("/jin").property("name", "jin") ;
				return null;
			}
		}) ;
		assertEquals(true, ListUtil.toList("hero", "jin").contains(session.pathBy("/bleujin").ref("friend").property("name").stringValue())) ;
	}
	
	
	public void testRefTos() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").refTos("friend", "/hero", "/jin", "/jin") ;
				wsession.pathBy("/hero").property("name", "hero").refTo("friend", "/air") ;
				wsession.pathBy("/jin").property("name", "jin").refTo("friend", "/air") ;
				wsession.pathBy("/air").property("name", "air").refTos("friend", "/novision") ;
				wsession.pathBy("/novision").property("name", "novision") ;
				return null;
			}
		}) ;

		session.pathBy("/bleujin").walkRefChildren("friend").strategy(TraversalStrategy.BreadthFirst).eachTreeNode(new WalkChildrenEach<Void>(){
			@Override
			public <T> T handle(WalkChildrenIterator trc) {
				Iterator<WalkReadNode> iter = trc.iterator() ;
				while(iter.hasNext()){
					WalkReadNode next = iter.next();
					Debug.line(next.level(), next.from(), next);
				}
				return null;
			}
		}) ; 
	}
	

	public void testRefsToMe() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("name", "bleujin").refTo("dept", "/dept/dev") ;
				wsession.pathBy("/hero").property("name", "hero").refTo("dept", "/dept/dev") ;
				wsession.pathBy("/dept/dev").property("name", "dev team") ;
				return null;
			}
		}) ;
		
		assertEquals(2, session.pathBy("/dept/dev").refsToMe("dept").find().size()) ;
		assertEquals(1, session.pathBy("/dept/dev").refsToMe("dept").fqnFilter("/emp").find().size()) ;
	}
	

	public void testRefsToMe2() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("name", "bleujin").refTos("dept", "/dept/cxm", "/dept/dev") ;
				wsession.pathBy("/hero").property("name", "hero").refTo("dept", "/dept/dev") ;
				wsession.pathBy("/dept/dev").property("name", "dev team") ;
				return null;
			}
		}) ;
		
		assertEquals(2, session.pathBy("/dept/dev").refsToMe("dept").find().size()) ;
		assertEquals(1, session.pathBy("/dept/dev").refsToMe("dept").fqnFilter("/emp").find().size()) ;
	}
	
	public void testRefsToMe3() throws Exception {
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emp/bleujin").property("name", "bleujin").refTo("dept", "/dept/dev") ;
				wsession.pathBy("/hero").property("name", "hero").refTo("dept", "/dept/dev") ;
				wsession.pathBy("/dept/dev").property("name", "dev team") ;
				return null;
			}
		}) ;
		
		assertEquals(2, session.pathBy("/dept/dev").refsToMe("dept").find().size()) ;
		assertEquals(1, session.pathBy("/dept/dev").refsToMe("dept").fqnFilter("/emp").find().size()) ;

		assertEquals(2, session.pathBy("/dept").refsToChildren("dept").find().size()) ;
		assertEquals(1, session.pathBy("/dept").refsToChildren("dept").fqnFilter("/emp").find().size()) ;

	}
	
	
	

	
}
