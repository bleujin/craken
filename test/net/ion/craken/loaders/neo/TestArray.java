package net.ion.craken.loaders.neo;

import java.util.Iterator;

import org.neo4j.graphdb.Node;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;

public class TestArray extends TestCase {

	private RepositoryImpl r;
	private ReadSession session;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest();
		r.defineWorkspaceForTest("neo", NeoWorkspaceConfig.createWithEmpty());
		r.start();
		this.session = r.login("neo");
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		r.shutdown();
	}

	public void testArrayProperty() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").append("city", "seoul", "sungnam", "busan");
				return null;
			}
		});
		
//		Debug.line(session.pathBy("/emps/bleujin").property("city").asSet());
		
		((NeoWorkspace)session.workspace()).debugPrint() ;
	}

	public void testRead() throws Exception {

		session.pathBy("/emps").children().debugPrint() ;
		session.pathBy("/emps/bleujin").toRows("name, city").debugPrint();
		Debug.line(session.pathBy("/emps/bleujin").property("city").asSet());
	}
	
	public void testDelete() throws Exception {
		Boolean result = session.tranSync(new TransactionJob<Boolean>() {
			@Override
			public Boolean handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").removeSelf() ;
				return null;
			}
		}) ;
		assertTrue(result) ;
	}

}
