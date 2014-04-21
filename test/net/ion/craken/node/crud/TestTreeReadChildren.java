package net.ion.craken.node.crud;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.TraversalStrategy;
import net.ion.framework.util.Debug;

public class TestTreeReadChildren extends TestBaseCrud{

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception { // /gparent/parent/child1, /gparent/parent/child2 
				wsession.pathBy("/gparent").property("name", "gparent")
				.child("parent1").property("name", "parent")
				.child("child11").property("name", "child1").parent() 
				.child("child12").property("name", "child2")
				.child("gchild").property("name", "gchild");
				
				wsession.pathBy("/gparent")
				.child("parent2").property("name", "parent")
				.child("child21").property("name", "child1").property("order", 1).parent() 
				.child("child22").property("name", "child2").property("order", 2)
				.child("gchild").property("name", "gchild") ;
				
				return null;
			}
		}) ;
	}
	
	public void testBreadthFirst() throws Exception {
		List<ReadNode> list = session.pathBy("/gparent").walkChildren().strategy(TraversalStrategy.BreadthFirst).includeSelf(true).toList() ; 
		assertEquals(9, list.size());
		assertEquals("gparent", list.get(0).property("name").asString());
		assertEquals("child1", list.get(8).property("name").asString());
	}
	

	public void testDepthFirst() throws Exception {
		long start = System.currentTimeMillis() ;
		List<ReadNode> list = session.pathBy("/gparent").walkChildren().strategy(TraversalStrategy.DepthFirst).includeSelf(true).toList() ;
		assertEquals(9, list.size());
		assertEquals("gparent", list.get(0).property("name").asString());
		assertEquals("gchild", list.get(8).property("name").asString());
		Debug.line(System.currentTimeMillis() - start );
	}
	
	public void testConnectWithFilter() throws Exception {
		List<ReadNode> list = session.pathBy("/gparent").walkChildren().strategy(TraversalStrategy.BreadthFirst).includeSelf(false).in("name", "parent", "child1").toList() ;
		
		assertEquals(4, list.size());
	}
	
	
	public void testSort() throws Exception {
		session.pathBy("/gparent").walkChildren().strategy(TraversalStrategy.BreadthFirst).includeSelf(false).descending("order").debugPrint();
	}

	
	public void testEachNode() throws Exception {
		session.pathBy("/gparent").walkChildren().strategy(TraversalStrategy.BreadthFirst).includeSelf(false).eq("name", "gchild").eachNode(new ReadChildrenEach<Void>() {
			@Override
			public Void handle(ReadChildrenIterator citer) {
				while(citer.hasNext()){
					Debug.line((WalkReadNode)citer.next());
				}
				return null;
			}
		}) ;
	}
	
	public void testEachTreeNode() throws Exception {
		session.pathBy("/gparent").walkChildren().strategy(TraversalStrategy.BreadthFirst).includeSelf(false).in("name", "parent", "child1").asTreeChildren().eachTreeNode(new WalkChildrenEach<Void>() {
			@Override
			public Void handle(WalkChildrenIterator trc) {
				for(WalkReadNode tnode :  trc){
					Debug.line(tnode);
				}
				return null;
			}
		}) ;
	}
	
	public void testReadChildrenIterator2ReadChildren() throws Exception {
		session.pathBy("/gparent").walkChildren().strategy(TraversalStrategy.BreadthFirst).includeSelf(false).in("name", "parent", "child1").asTreeChildren().eachTreeNode(new WalkChildrenEach<Void>() {
			@Override
			public Void handle(WalkChildrenIterator trc) {
				ReadChildren rc = trc.toReadChildren().startsWith("name", "child");
				for(ReadNode tnode :  rc){
					Debug.line(tnode);
				}
				return null;
			}
		}) ;
	}
	
	
	
}
