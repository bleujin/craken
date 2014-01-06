package net.ion.craken.node.crud;

import java.util.Set;

import net.ion.craken.node.ReadNode;
import net.ion.craken.tree.PropertyId;

public class TestFirst extends TestBaseCrud {


	public void testLoad() throws Exception {
		ReadNode node = session.pathBy("/") ;
		Set<PropertyId> keys = node.keys() ;
		
		assertEquals(0, keys.size()) ;
	}
	
	public void testExists() throws Exception {
		assertEquals(true, session.exists("/")) ;
		
		ReadNode node = session.pathBy("/") ;
		ReadNode root = session.root() ;
		
		assertEquals(true, node.equals(root)) ;
		
		assertEquals(true, session.exists("/")) ;
		assertEquals(false, session.exists("/test")) ;
	}
	
	
	
}
