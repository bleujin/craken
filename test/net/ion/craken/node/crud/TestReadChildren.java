package net.ion.craken.node.crud;

import java.sql.ResultSetMetaData;
import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.db.Page;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;

public class TestReadChildren extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(TransactionJobs.dummy("/bleujin", 200)).get() ;
	}
	
	public void testFirst() throws Exception {
		assertEquals(200, session.pathBy("/bleujin").children().toList().size()) ;
	}
	
	public void testSkip() throws Exception {
		assertEquals(3, session.pathBy("/bleujin").children().skip(5).offset(3).toList().size()) ;
	}
	
	public void testFilter() throws Exception {
		List<ReadNode> list = session.pathBy("/bleujin").children().gt("dummy", 3).toList();
		
		assertEquals(196, list.size()) ; // default offset = 1000
		for (ReadNode readNode : list) {
			assertEquals(true, ((Integer)readNode.property("dummy").value()) > 3) ;
		}

		list = session.pathBy("/bleujin").children().offset(200).gt("dummy", 3).toList();
		assertEquals(196, list.size()) ; // default offset = 100

	}
	
	public void testSorting() throws Exception {
		List<ReadNode> list = session.pathBy("/bleujin").children().ascending("dummy").skip(5).offset(2).toList();
		assertEquals(2, list.size()) ;
		assertEquals(5, list.get(0).property("dummy").value()) ;
		assertEquals(6, list.get(1).property("dummy").value()) ;
	}
	
	public void testFilterWithSort() throws Exception {
		List<ReadNode> list = session.pathBy("/bleujin").children().gt("dummy", 3).skip(5).offset(2).ascending("dummy").toList();

		assertEquals(2, list.size()) ;
		assertEquals(9, list.get(0).property("dummy").value()) ;
	}
	
	public void testFilterWithNoSort() throws Exception {
		List<ReadNode> list = session.pathBy("/bleujin").children().gt("dummy", 3).skip(5).offset(10).toList();
		assertEquals(10, list.size()) ;
		
		for (ReadNode readNode : list) {
			assertEquals(true, ((Integer)readNode.property("dummy").value()) > 3) ;
		}
	}
	
	
	public void testToPageRows() throws Exception {
		final Rows rows = session.pathBy("/bleujin").children().ascending("dummy").toRows(Page.create(10, 2), "dummy");
		rows.debugPrint() ;
		
		ResultSetMetaData meta = rows.getMetaData();
		for ( int i =1 ; i <= meta.getColumnCount() ; i++){
			Debug.line(meta.getColumnLabel(i)) ;
		}
	}
	
	
	public void testIgnoredWhenNotFoundProperty() throws Exception {
		final Rows rows = session.pathBy("/bleujin").children().ascending("Dummy").toRows(Page.create(10, 2), "dummy, name");
		rows.debugPrint() ;
	}
	
	
	public void testFirstNode() throws Exception {
		ReadNode readNode = session.pathBy("/bleujin").children().lte("dummy", 5).descending("dummy").firstNode();
		assertEquals(5, readNode.property("dummy").intValue(0)) ;
		
	}
	
}
