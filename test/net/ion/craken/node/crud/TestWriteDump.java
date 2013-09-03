package net.ion.craken.node.crud;

import net.ion.craken.node.DumpJob;
import net.ion.craken.node.DumpSession;

public class TestWriteDump extends TestBaseCrud {

	public void testInterface() throws Exception {
		
		session.dump(new DumpJob<Void>(){
			@Override
			public Void handle(DumpSession dsession) throws Exception {
				for (int i = 0 ; i < 10 ; i++) {
					dsession.createBy("/bleujin/" + i).property("name", "bleujin").property("index", i) ;
					dsession.continueUnit() ;
				}
				return null;
			}
		}).get() ;
		
		assertEquals(10, session.pathBy("/bleujin").children().toList().size()) ;
	}
	
}
