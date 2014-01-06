package net.ion.bleuin.neo4j;

import net.ion.neo.bleujin.TestTraverse.RelTypes;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import junit.framework.TestCase;

public class TestNeo extends TestCase{

	private GraphDatabaseService graphDB;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.graphDB = new GraphDatabaseFactory().newEmbeddedDatabase("./resource/neo4j");
	}
	
	@Override
	protected void tearDown() throws Exception {
		graphDB.shutdown() ;
		super.tearDown();
	}
	
	public void testFirst() throws Exception {

		Transaction tx = graphDB.beginTx();
		
		Node firstNode = graphDB.createNode();
		firstNode.setProperty("message", "Hello, ") ;
		Node secondNode = graphDB.createNode();
		secondNode.setProperty("message", "World!") ;
		
		Relationship relationShip = firstNode.createRelationshipTo(secondNode, RelTypes.KNOWS);
		relationShip.setProperty("message", "brave Neo4j") ;
		tx.success() ;
		tx.finish() ;
	}
}
