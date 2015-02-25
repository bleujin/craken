package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TraversalStrategy;
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

		session.pathBy("/bleujin").walkRefChildren("friend").strategy(TraversalStrategy.BreadthFirst).debugPrint(); 
		
		
		
	}
	
	
}
