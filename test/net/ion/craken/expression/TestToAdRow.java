package net.ion.craken.expression;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.crud.util.TransactionJobs;
import net.ion.framework.db.Page;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;
import net.ion.rosetta.Parser;

public class TestToAdRow extends TestBaseCrud {

	public void testToAdRows() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20) ;
				return null;
			}
		}) ;
		
		Rows rows = session.root().children().toAdRows("this.name b, this.age");
		rows.debugPrint() ;
	}

	public void testPage2() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 120)) ;

		assertEquals(10, session.pathBy("/bleujin").children().ascending("dummy").toAdRows(Page.create(10, 2, 10), "this.name b, dummy, this.age").firstRow().getInt("dummy")) ; 
		assertEquals(90, session.pathBy("/bleujin").children().ascending("dummy").toAdRows(Page.create(10, 10, 10), "this.name b, dummy, this.age").firstRow().getInt("dummy")) ;
		
	}

	public void testOld() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 120)) ;
		
		Rows rows = session.pathBy("/bleujin").children().ascending("dummy").toAdRows(Page.create(10, 11, 10), "this.name b, dummy, this.age");
		assertEquals(20, rows.firstRow().getInt("cnt")) ;
		assertEquals(100, rows.firstRow().getInt("dummy")) ;

		rows = session.pathBy("/bleujin").children().ascending("dummy").toAdRows(Page.create(10, 5, 10), "this.name b, dummy, this.age");
		assertEquals(101, rows.firstRow().getInt("cnt")) ;
		assertEquals(40, rows.firstRow().getInt("dummy")) ;

	}
	
	public void testPageToAdRows() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 120)) ;
		
		Rows rows = session.pathBy("/bleujin").children().ascending("dummy").toAdRows(Page.create(10, 11, 10), "this.name b, dummy, this.age");


		assertEquals(20, rows.firstRow().getInt("cnt")) ;
		assertEquals(100, rows.firstRow().getInt("dummy")) ;

		rows = session.pathBy("/bleujin").children().ascending("dummy").toAdRows(Page.create(10, 5, 10), "this.name b, dummy, this.age");
		assertEquals(101, rows.firstRow().getInt("cnt")) ;
		assertEquals(40, rows.firstRow().getInt("dummy")) ;

		rows = session.pathBy("/bleujin").children().ascending("dummy").toAdRows(Page.create(10, 13, 10), "this.name b, dummy, this.age");
		assertEquals(0, rows.getRowCount()) ;

		rows = session.pathBy("/bleujin").children().ascending("dummy").skip(10).toAdRows(Page.create(10, 5, 10), "this.name b, dummy, this.age");
		assertEquals(101, rows.firstRow().getInt("cnt")) ;
		assertEquals(50, rows.firstRow().getInt("dummy")) ;
	}
	
	public void testCaseWhenParser() throws Exception {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		
		SelectProjection sp = TerminalParser.parse(parser, "case when /*+ comment */ (this.age = 20) then 'self' else 'other' end as name");
//		SelectProjection sp = TerminalParser.parse(parser, "case when /*+ comment */ (this.age > 20) then 'self' else 'other' end as name");
		
		Debug.line(sp) ;
		
		
	}
	
	public void testCaseWhen() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 10)) ;
		Rows rows = session.pathBy("/bleujin").children().toAdRows(Page.TEN, "(case when this.name='bleujin' then true else false end) as isbleujin");
//		rows.debugPrint() ;
		assertEquals(true, rows.firstRow().getBoolean("isbleujin")) ;
	}
	
	public void testFunction() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").refTo("dept", "/dept/dev") ;
				wsession.pathBy("/dept/dev").property("name", "dev").refTo("manager", "/emps/bleujin") ;
				return null;
			}
		}) ;
		Rows rows = session.pathBy("/emps").children().toAdRows("substring(this.name, 2) s");
		rows.debugPrint() ;
	}
	
	public void testRelation() throws Exception {
		session.tranSync(new TransactionJob<Void>(){
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/emps/bleujin").property("name", "bleujin").refTo("dept", "/dept/dev") ;
				wsession.pathBy("/dept/dev").property("name", "dev").refTo("manager", "/emps/bleujin") ;
				return null;
			}
		}) ;
		
		Rows rows = session.pathBy("/emps").children().toAdRows("dept.manager.name managerName");
		rows.debugPrint() ;
		
		session.pathBy("/emps/bleujin").toRows("this.dept.manager.name managerName") ;
	}
	
	
}

//class FirstRowHandler implements ResultSetHandler<Row>{
//
//	private static final long serialVersionUID = -6786579511226655817L;
//	public final static FirstRowHandler SELF = new FirstRowHandler() ;
//	
//	public Row handle(ResultSet rs) {
//		try {
//			if (!rs.first()) {
//				throw RepositoryException.throwIt("No Data Found\n");
//			}
//			
//			return Row.create(RowsUtils.currentRowToMap(rs), getColumnsNames(rs));
//		} catch (SQLException ex) {
//			throw RepositoryException.throwIt(ex);
//		}
//	}
//	
//
//	private static String[] getColumnsNames(ResultSet rs) throws SQLException {
//		ResultSetMetaData meta = rs.getMetaData();
//		String[] names = new String[meta.getColumnCount()];
//
//		for (int i = 0; i < names.length; ++i) {
//			names[i] = meta.getColumnName(i + 1).toUpperCase();
//		}
//
//		return names;
//	}
//
//}
