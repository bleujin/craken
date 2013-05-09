package net.ion.craken.node.crud;

import java.sql.ResultSetMetaData;
import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.search.util.TransactionJobs;
import net.ion.framework.db.Page;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;

import com.google.common.base.Predicate;

public class TestReadChildren extends TestBaseCrud {

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tran(TransactionJobs.dummy("/bleujin", 200)).get() ;
	}
	
	public void testFirst() throws Exception {
		assertEquals(10, session.pathBy("/bleujin").children().toList().size()) ;
	}
	
	public void testSkip() throws Exception {
		assertEquals(3, session.pathBy("/bleujin").children().skip(5).offset(3).toList().size()) ;
	}
	
	public void testFilter() throws Exception {
		final List<ReadNode> list = session.pathBy("/bleujin").children().filter(new Predicate<ReadNode>() {
			@Override
			public boolean apply(ReadNode node) {
				return new Integer(3).compareTo((Integer)node.property("dummy").value()) < 0; // > 3
			}
		}).toList();
		
		assertEquals(6, list.size()) ;
		for (ReadNode readNode : list) {
			assertEquals(true, ((Integer)readNode.property("dummy").value()) > 3) ;
		}
	}
	
	public void testSorting() throws Exception {
		List<ReadNode> list = session.pathBy("/bleujin").children().ascending("dummy").skip(5).offset(2).toList();
		assertEquals(2, list.size()) ;
		assertEquals(5, list.get(0).property("dummy").value()) ;
		assertEquals(6, list.get(1).property("dummy").value()) ;
	}
	
	public void testComposite() throws Exception {
		List<ReadNode> list = session.pathBy("/bleujin").children().filter(new Predicate<ReadNode>() {
			@Override
			public boolean apply(ReadNode node) {
				return new Integer(3).compareTo((Integer)node.property("dummy").value()) < 0; // > 3
			}
		}).skip(5).offset(2).descending("dummy").toList();

		assertEquals(1, list.size()) ;
		assertEquals(4, list.get(0).property("dummy").value()) ;
	}
	
	
	public void testToPageRows() throws Exception {
		final Rows rows = session.pathBy("/bleujin").children().ascending("dummy").toRows(Page.create(10, 2), "dummy");
		rows.debugPrint() ;
		
		ResultSetMetaData meta = rows.getMetaData();
		for ( int i =1 ; i <= meta.getColumnCount() ; i++){
			Debug.line(meta.getColumnLabel(i)) ;
		}
	}
	
	
	
}
