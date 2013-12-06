package net.ion.craken.node.dist;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TranResult;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;

public class TestCacheStore extends TestCase  {

	
	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		this.session = r.login("test");
	}
	
	@Override
	protected void tearDown() throws Exception {
		r.shutdown() ;
		super.tearDown();
	}
	
	public void testCallCountWhenCreate() throws Exception {

		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				for (int i = 0; i < 10; i++) {
					wsession.createBy("/emp/" + i).property("name", i).property("age",i) ;
				}
				return null;
			}
		}) ;
		
		Debug.line(session.attribute(TranResult.class.getCanonicalName())) ;
		session.pathBy("/emp/3").toRows("name, age").debugPrint() ;
	}
	
	
	
}
