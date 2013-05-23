package net.ion.craken.backup;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;

public class TestRestore extends TestBaseCrud {
	
	public void xtestBackupData() throws Exception {
		session.getWorkspace().addListener(MongoBackupListener.test()) ;
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).addChild("address").property("city", "seoul").property("postcode", 100) ;
				wsession.pathBy("/hero").property("name", "hero").property("age", 30).refTo("friend", "/bleujin") ;
				wsession.pathBy("/jin").property("name", "jin").property("age", 30).refTo("friend", "/bleujin") ;
				return null;
			}
		}) ;
		
	}
	
	public void testRestore() throws Exception {
		Restoreable re = MongoBackupListener.test();
		re.restore(session) ;
		
		assertEquals(3, session.root().children().toList().size()) ;
	}
	
	public void testProperties() throws Exception {
		Restoreable re = MongoBackupListener.test();
		re.restore(session) ;
		
		assertEquals(true, session.exists("/bleujin")) ;
		
		assertEquals("bleujin", session.pathBy("/bleujin").property("name").value()) ;
		assertEquals(20, session.pathBy("/bleujin").property("age").value()) ;
	}
	
	public void testRelation() throws Exception {
		Restoreable re = MongoBackupListener.test();
		re.restore(session) ;

		assertEquals("bleujin", session.pathBy("/hero").ref("friend").property("name").value()) ; 
	}
	

}
