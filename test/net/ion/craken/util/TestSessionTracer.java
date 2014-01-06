package net.ion.craken.util;

import java.io.PrintStream;

import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.util.TransactionJobs;

public class TestSessionTracer extends TestBaseCrud{

	public void testInitTracer() throws Exception {
		
		session.tranSync(TransactionJobs.dummy("/bleujin", 10)) ;
		PrintStream print = System.out;
		session.credential().tracer(print) ;
		
		session.pathBy("/bleujin").children().debugPrint() ;
		session.root().childQuery("").find().debugPrint() ;
	}
	
	
	
	
	
	
	
	
	
}
