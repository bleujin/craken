package net.ion.craken.loaders.neo;

import java.io.File;
import java.util.Iterator;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;

import org.neo4j.graphdb.Node;

public class TestNeoWorkspaceStore extends TestCase {

	
	private RepositoryImpl r;
	private ReadSession session;

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
//		FileUtil.deleteDirectory(new File(NeoWorkspaceConfig.create().neoLocation()));
		
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
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").property("age", 20).append("loc", "seoul", "sungnam") ;
				return null;
			}
		}) ;
	}
	
	public void testRead() throws Exception {
		session.pathBy("/emps/bleujin").toRows("name, age, loc").debugPrint() ;
	}
	
	public void testConfirm() throws Exception {
		Iterator<Node> iter = ((NeoWorkspace)session.workspace()).graphDB().getAllNodes().iterator();
		while(iter.hasNext()){
			Debug.line(iter.next()) ;
		}
	}
	
}
