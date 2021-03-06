package net.ion.craken.node.convert.rows;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.ChildQueryResponse;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.search.TestBaseSearch;
import net.ion.framework.db.Row;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

public class TestToRowsSort extends TestBaseSearch {
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				for (int i : ListUtil.rangeNum(50)) {
					wsession.root().child("/board1/" +  i).property("index", i).property("name", "board1").property("writer",  "hijin").child("address").property("city", "seoul")
					.parent().refTos("register", "/users/bleujin").root().child("/board1").property("name", "free") ;
				}
				for (int i : ListUtil.rangeNum(50)) {
					wsession.root().child("/board2/" +  i).property("index", i).property("name", "board2").property("writer", "hero") ;
				}
				return null;
			}
		}) ;
		
		
	}
	public void testChildProperty() throws Exception {
		long start = System.currentTimeMillis() ;
		final ChildQueryResponse find = session.pathBy("/board1").childQuery("").descendingNum("index").skip(10).offset(2).find();
		long mid = System.currentTimeMillis() ;
		Rows rows = find.toRows("name, substring(writer, 2) writer, index, address.city acity, parent.name boardname") ;
		
		assertEquals(2, rows.getRowCount()) ;
		
		Row first = rows.firstRow();
		assertEquals(39, first.getInt("index")) ;
		assertEquals("board1", first.getString("name")) ;
		assertEquals("jin", first.getString("writer")) ;
		assertEquals("seoul", first.getString("acity")) ;
		assertEquals("free", first.getString("boardname")) ;
		
		Debug.line(System.currentTimeMillis() - start, System.currentTimeMillis() - mid) ;
	}
	

	
	public void testRefTo() throws Exception {
//		session.queryRequest("").ascending("index").skip(10).offset(2).refTo("register", Fqn.fromString("/users/bleujin")).find().debugPrint() ;
		
		Rows rows = session.queryRequest("").descendingNum("index").skip(10).offset(2)
			.refTo("register", Fqn.fromString("/users/bleujin")).find().toRows("name, substring(this.writer, 2) writer, index, address.city acity, address.city, register.age age") ;
		
		Row first = rows.firstRow();
		assertEquals(39, first.getInt("index")) ;
		assertEquals("board1", first.getString("name")) ;
		assertEquals("jin", first.getString("writer")) ;
		assertEquals("seoul", first.getString("acity")) ;
		assertEquals("seoul", first.getString("city")) ;
		assertEquals(true, first.getObject("age") == null) ;
	}
	
	
	
	public void testRefProperty() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) {
				wsession.pathBy("/users/bleujin").property("age", 20) ;
				return null;
			}
		}) ;
		
		final ChildQueryResponse find = session.pathBy("/board1").childQuery("").descendingNum("index").skip(10).offset(2).find();
		Rows rows = find.toRows("name, substring(writer, 2) writer, index, address.city acity, register.age age") ;
		
		Row first = rows.firstRow();
		assertEquals(39, first.getInt("index")) ;
		assertEquals("board1", first.getString("name")) ;
		assertEquals("jin", first.getString("writer")) ;
		assertEquals("seoul", first.getString("acity")) ;
		assertEquals(20, first.getInt("age")) ;
	}
	
	
	
	public void xtestLoop() throws Exception {
		for (int i = 0; i < 20; i++) {
			testChildProperty() ;
		}
	}
	
}
