package net.ion.craken.node.search;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.Craken;
import net.ion.craken.node.crud.util.TransactionJobs;

import com.sun.corba.se.impl.activation.RepositoryImpl;

public class TestEvictionInSearch extends TestCase{

	
	public void testEviction() throws Exception {
		Craken r = Craken.create();
		r.createWorkspace("test") ;
		r.start() ;

		
		ReadSession session = r.login("test");
		
		session.tranSync(TransactionJobs.dummyEmp(20)) ;
		
		
		session.pathBy("/emp").children().debugPrint() ;
		
		
		r.shutdown() ;
		
	}
}
