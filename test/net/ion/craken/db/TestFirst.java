package net.ion.craken.db;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import com.google.common.base.Function;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.TransactionJob;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.rows.CrakenNodeRows;
import net.ion.craken.node.crud.TestBaseCrud;
import net.ion.framework.db.DBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.bean.handlers.MapListHandler;
import net.ion.framework.db.manager.DBManager;
import net.ion.framework.db.procedure.IParameterQueryable;
import net.ion.framework.db.procedure.IUserProcedure;
import net.ion.framework.db.servant.IExtraServant;
import net.ion.framework.db.servant.StdOutServant;
import net.ion.framework.util.Debug;
import net.ion.radon.impl.filter.SystemOutLog;

public class TestFirst extends TestBaseCrud {

	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	
	public void testReg() throws Exception {
		String rexEx = "dummy@findPerson" ;
		
		Debug.line("dummy@findPerson".matches(rexEx)) ;
	}
	
	public void testInterface() throws Exception {

		CrakenManager dbm = new CrakenManager(this.r) ;
		dbm.register("dummy@.+", new QueryExecuteFunction() {
			
			@Override
			public Rows query(ReadSession session, QueryParam query) throws SQLException {
				if (query.procName().endsWith("findPerson")){
					String name = query.getString(0) ;
					return session.root().child(name).toRows("name", "age");
				}
				throw new IllegalArgumentException("not registered query : " + query.procName() ) ;
			}

			@Override
			public int execute(ReadSession session, final QueryParam query) throws SQLException, InterruptedException, ExecutionException {
				
				if (query.procName().endsWith("addPerson")){
					session.tranSync(new TransactionJob<Void>() {
						@Override
						public Void handle(WriteSession wsession) {
							wsession.pathBy(query.getString(0)).property("name", query.getString(0)).property("age", query.getInt(1)).property("address", query.getString(2)) ;
							return null ;
						}
					}) ;
					return 1 ;
				}
				
				return 0;
			}
		}) ;
		
		
		
		
		DBController dc = new DBController("craken", dbm, new StdOutServant());
		dc.initSelf() ;
		
		dc.createUserProcedure("dummy@addPerson(?,?,?)").addParam("bleujin").addParam(20).addParam("seoul").execUpdate() ;
		dc.createUserProcedure("dummy@addPerson(?,?,?)").addParam("hero").addParam(20).addParam("busan").execUpdate() ;

		Rows rows = dc.createUserProcedure("dummy@findPerson(?)").addParam("hero").execQuery() ;
		rows.debugPrint() ;

		rows.beforeFirst() ;
		List<Map<String, ? extends Object>> list = rows.toHandle(new MapListHandler());
		Debug.line(list) ;
		
		dc.destroySelf() ;
		
	}
}
