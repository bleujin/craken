package net.ion.bleujin.infinispan;

import junit.framework.TestCase;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;

public class TestFirst extends TestCase {


	public void testCreate() throws Exception {
		RepositoryImpl r = RepositoryImpl.create();
		r.defineWorkspace("test") ;
		r.defineWorkspace("test2") ;
		r.start() ;
		
		r.login("test2").root().children().debugPrint() ;
		
		ReadSession session = r.login("test");
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/dept/dev").property("name", "developer") ;
				
				wsession.pathBy("/dev/bleujin").property("name", "bleujin").property("age", 20).property("text", "태극기가 바람에 펄럼입니다.").child("address").property("num", 1).property("juso", "seoul") ;
				wsession.pathBy("/dev/ryunhee").property("name", "ryunhee").property("age", 20).refTo("dept", "/dept/dev") ;
				return null;
			}
		}) ;
		
		final ReadNode found = session.pathBy("/dev").childQuery("태극기").findOne();
		
		assertEquals("bleujin", found.property("name").stringValue()) ;
		assertEquals("seoul", found.child("address").property("juso").stringValue()) ;
		
		
		ReadNode ryun = session.pathBy("/dev/ryunhee");
		assertEquals("developer", ryun.ref("dept").property("name").stringValue()) ;
		
		
		r.shutdown() ;
	}
}


