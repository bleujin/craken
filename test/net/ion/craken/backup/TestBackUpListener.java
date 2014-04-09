package net.ion.craken.backup;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.util.Debug;

public class TestBackUpListener extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.workspace().addListener(new MongoBackupListener("61.250.201.78", 27017, "craken", "craken")) ;
	}
	
	public void testRegister() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).child("address").property("city", "seoul").property("postcode", 100) ;
				
				return null;
			}
		}) ;
		
	}
	
	public void testUpdate() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20).child("address").property("city", "seoul").property("postcode", 100) ;
				return null;
			}
		}) ; // insert 
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("color", "blue") ;
				return null;
			}
		}) ; // update 
		Debug.line(session.pathBy("/bleujin").transformer(Functions.toPropertyValueMap())) ;
		
	}
	
}
