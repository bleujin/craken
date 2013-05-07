package net.ion.craken.node.crud;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.search.util.TransactionJobs;

public class TestReadChildren extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(TransactionJobs.dummy("/bleujin", 10)).get() ;
	}
	
	public void testFirst() throws Exception {
		session.pathBy("/bleujin").children().debugPrint() ;
	}
	
	
}
