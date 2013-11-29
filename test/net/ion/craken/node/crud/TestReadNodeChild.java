package net.ion.craken.node.crud;

import net.ion.craken.node.crud.util.TransactionJobs;

public class TestReadNodeChild extends TestBaseCrud {

	
	public void testReadChild() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 3)) ;
		
		session.pathBy("/bleujin").child("0").toRows("name, dummy").debugPrint() ;
		session.pathBy("/bleujin").child("1").toRows("name, dummy").debugPrint() ;
		session.pathBy("/bleujin").child("2").toRows("name, dummy").debugPrint() ;
	}
}
