package net.ion.craken.loaders.neo;

import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.neo.bleujin.TestTraverse;
import net.ion.neo.bleujin.TestTraverse.RelTypes;

import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.RelationshipType;
import org.neo4j.graphdb.Transaction;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;

import junit.framework.TestCase;

public class TestNeo extends TestCase {

	
	private Transaction tx;
	private GraphDatabaseService gdb;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		String location = "./resource/neo";
		this.gdb = new GraphDatabaseFactory().newEmbeddedDatabase(location);
		this.tx = gdb.beginTx();
	}
	
	@Override
	protected void tearDown() throws Exception {
		tx.success() ;
		tx.finish() ;
		super.tearDown();
	}
	
	public void testFirst() throws Exception {
		Node root = gdb.getNodeById(0);
		Node node = gdb.createNode();
		node.setProperty("list", new String[]{"bleujin", "jin", "hero"}) ;
		root.createRelationshipTo(node, createRelType("list")) ;
	}
	
	
	public void testRead() throws Exception {
		Node root = gdb.getNodeById(0);
		Node targeetNode = root.getSingleRelationship(createRelType("list"), Direction.OUTGOING).getEndNode() ;
		Debug.line(targeetNode.getProperty("list")) ;
	}


	private RelationshipType createRelType(final String rel) {
		return new RelationshipType() {
			@Override
			public String name() {
				return rel;
			}
		};
	}
	
	
	
}
