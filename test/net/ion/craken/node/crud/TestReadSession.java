package net.ion.craken.node.crud;

import net.ion.craken.node.Credential;
import net.ion.craken.node.ReadNode;
import net.ion.framework.util.Debug;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.util.Version;

public class TestReadSession extends TestBaseCrud {

	public void testSessionKey() throws Exception {
		assertEquals(Credential.EMANON.accessKey(), session.credential().accessKey()) ; 
		assertEquals(true, session.credential().secretKey() == null) ; // clear after login 
	}
	
	public void testPathBy() throws Exception {
		assertEquals(false, session.root().hasChild("/bleujin")) ;
		try {
			assertEquals(true, session.pathBy("/bleujin"));
			fail() ;
		} catch(IllegalArgumentException expect){
		}
		assertEquals(false, session.root().hasChild("/bleujin")) ;
	}
	
	public void testNotFoundPathThrowIllegalException() throws Exception {
		assertEquals(false, session.root().hasChild("/notfound")) ;
		try {
			assertEquals(true, session.root().child("/notfound") != null);
			fail() ;
		} catch(IllegalArgumentException expect){
		}
		assertEquals(false, session.root().hasChild("/notfound")) ;
	}
	
	public void testRoot() throws Exception {
		assertEquals(true, session.exists("/")) ;
	}
	
	
	public void testNotFoundPath2ThrowIllegalException() throws Exception {
		assertEquals(false, session.exists("/notfound")) ;

		try {
			session.pathBy("/notfound") ;
			fail() ;
		} catch(IllegalArgumentException expect){
		}
	}

	
	public void testNotFoundPath2Ignore() throws Exception {
		assertEquals(false, session.exists("/notfound")) ;
		ReadNode fake = session.ghostBy("/notfound");
		
		assertEquals(true, fake != null) ;
		assertEquals(0, fake.children().toList().size()) ;

		ReadNode ghost = session.ghostBy("/notfound");
		
		assertEquals(true, ghost != null) ;
		assertEquals(0, ghost.children().toList().size()) ;

	
	}
	
	
	public void testMyAnalyzer() throws Exception {
		Debug.line(session.queryAnalyzer()) ; 
		assertEquals(true, (session.queryAnalyzer() instanceof Analyzer) ? true : false) ;
		
		r.login("test", new StandardAnalyzer(Version.LUCENE_CURRENT)) ;
	}

	
}
