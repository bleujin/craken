package net.ion.craken.expression;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;

import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.rows.AdNodeRows;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.craken.node.search.util.TransactionJobs;
import net.ion.framework.db.Page;
import net.ion.framework.db.RepositoryException;
import net.ion.framework.db.Row;
import net.ion.framework.db.Rows;
import net.ion.framework.db.RowsUtils;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.db.bean.handlers.FirstRowHandler;
import net.ion.framework.parse.gson.JsonParser;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;
import net.ion.rosetta.Parser;

public class TestParser extends TestBaseCrud {

	public void testWhere() throws Exception {
		session.tranSync(new TransactionJob<Void>() {
			@Override
			public Void handle(WriteSession wsession) throws Exception {
				wsession.pathBy("/bleujin").property("name", "bleujin").property("age", 20);
				return null;
			}
		});

		assertEquals(1, session.root().children().where("age >= 20").where("name = 'bleujin'").toList().size());
		assertEquals(1, session.root().children().where("age >= 20 and name = 'bleujin'").toList().size());
		assertEquals(1, session.root().children().where("(case when this.name = 'bleujin' then this.age else 0 end) > 0 ").toList().size());
	}

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

	public void testOld() throws Exception {
		session.tranSync(TransactionJobs.dummy("/bleujin", 120)) ;
		
		Rows rows = session.pathBy("/bleujin").children().ascending("dummy").toRows(Page.create(10, 11, 10), "this.name b", "dummy", "this.age");
		assertEquals(20, rows.firstRow().getInt("cnt")) ;
		assertEquals(100, rows.firstRow().getInt("dummy")) ;

		rows = session.pathBy("/bleujin").children().ascending("dummy").toRows(Page.create(10, 5, 10), "this.name b", "dummy", "this.age");
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
}
