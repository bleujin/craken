package net.ion.craken.loaders.neo;

import java.io.File;
import java.util.Iterator;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import junit.framework.TestCase;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.util.Debug;
import net.ion.framework.util.FileUtil;

public class TestRoot extends TestCase {

	public void testRootInNeo() throws Exception {
		String dbPath = "./resource/neo"; 
		FileUtil.deleteDirectory(new File(dbPath)) ;
		
		final GraphDatabaseService graphDB = new GraphDatabaseFactory().newEmbeddedDatabase(dbPath);
		
		
		Node root = graphDB.getNodeById(0L) ;
		Debug.line(root) ;
		
		
		Transaction tx = graphDB.beginTx();
		Node firstNode = graphDB.createNode();
		firstNode.setProperty("__id", "/") ;
		tx.success() ;
		tx.finish() ;
		
		

		Thread th = new Thread(){
			public void run(){
				Iterator<Node> iter = graphDB.getAllNodes().iterator();
				while(iter.hasNext()){
					Node node = iter.next();
					Debug.line(node, node.getPropertyKeys(), node.getPropertyValues()) ;
				}
			}
		};
		
		
		th.start() ;
		th.join() ;
		
		graphDB.shutdown() ;

	}
	
	public void testRootInWorkspace() throws Exception {

		RepositoryImpl r = RepositoryImpl.inmemoryCreateWithTest();
		r.defineWorkspaceForTest("neo", NeoWorkspaceConfig.createWithEmpty());
		r.start();
		ReadSession session = r.login("neo");
		
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").property("name", "bleujin") ;
				return null;
			}
		}) ;
		
		r.shutdown() ;
	}

}
