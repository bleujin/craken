package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import junit.framework.TestCase;

public class TestRefTreeReadChildren extends TestBaseCrud {

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
		
		
		assertEquals(6, session.pathBy("/bleujin").refTreeChildren("friend").includeSelf(true).count()) ; 
	}
	
	
	public void testSetLimitWhenInfinityLoop() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").refTos("friend", "/hero", "/jin") ;
				wsession.pathBy("/jin").property("name", "jin").refTo("friend", "/bleujin") ;
				return null;
			}
		}) ;
		
		assertEquals(4, session.pathBy("/bleujin").refTreeChildren("friend").loopLimit(3).includeSelf(true).count()) ;
	}
	
	
}
