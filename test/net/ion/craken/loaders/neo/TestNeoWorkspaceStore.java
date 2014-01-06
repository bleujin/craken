package net.ion.craken.loaders.neo;

import java.util.Iterator;

import org.neo4j.graphdb.Node;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import junit.framework.TestCase;

public class TestNeoWorkspaceStore extends TestCase {

	
	private RepositoryImpl r;
	private ReadSession session;

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.r = RepositoryImpl.inmemoryCreateWithTest() ;
		r.defineWorkspaceForTest("neo", NeoWorkspaceConfig.create()) ;
		r.start() ;
		this.session = r.login("neo") ;
	}
	
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		r.shutdown() ;
	}
	
	public void testCreate() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}) ;
	}
	
	public void testRead() throws Exception {
		session.pathBy("/emps/bleujin").toRows("name, age").debugPrint() ;
	}
	
	public void testConfirm() throws Exception {
		Iterator<Node> iter = ((NeoWorkspace)session.workspace()).graphDB().getAllNodes().iterator();
		while(iter.hasNext()){
			Debug.line(iter.next()) ;
		}
	}
	
}
