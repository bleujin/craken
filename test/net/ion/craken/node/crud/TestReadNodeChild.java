package net.ion.craken.node.crud;

import java.util.Set;

import net.ion.craken.node.crud.util.TransactionJobs;

public class TestReadNodeChild extends TestBaseCrud {

	
	public void testReadChild() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 3)) ;
		
		session.pathBy("/bleujin").child("0").toRows("name, dummy").debugPrint() ;
		session.pathBy("/bleujin").child("1").toRows("name, dummy").debugPrint() ;
		session.pathBy("/bleujin").child("2").toRows("name, dummy").debugPrint() ;
	}
	
	public void testChildNames() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 3)) ;
		
		Set<String> names = session.pathBy("/bleujin").childrenNames() ;
		assertEquals(true, names.contains("0"));
		assertEquals(true, names.contains("1"));
		assertEquals(true, names.contains("2"));
	}
}
