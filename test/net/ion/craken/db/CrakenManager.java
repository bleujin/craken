package net.ion.craken.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.RepositoryImpl;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.manager.DBManager;
import net.ion.framework.db.procedure.RepositoryService;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

public class CrakenManager extends DBManager {

	private CrakenRepositoryService cservice ;
	private RepositoryImpl repository ;
	public CrakenManager(RepositoryImpl repository){
		this.repository = repository ;
		this.cservice = new CrakenRepositoryService(this) ;
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		throw new UnsupportedOperationException() ;
	}

	@Override
	public int getDBManagerType() {
		return 77;
	}

	@Override
	public String getDBType() {
		return "craken";
	}
	
	public ReadSession session(){
		return repository.testLogin("test") ;
//		return repository.testLogin("test") ;
	}

	@Override
	public RepositoryService getRepositoryService() {
		return cservice;
	}

	@Override
	protected void myDestroyPool() throws Exception {
		
	}
	
	protected void heartbeatQuery(IDBController dc) throws SQLException {
		
	}
	
	@Override
	protected void myInitPool() throws SQLException {
		
	}

	private Map<String, QueryFunction> querys = MapUtil.newMap() ;
	private Map<String, ExecuteFunction> executes = MapUtil.newMap() ;
	public CrakenManager register(String regPattern, QueryFunction function) {
		querys.put(regPattern, function) ;
		return this ;
	}
	public CrakenManager register(String regPattern, ExecuteFunction function) {
		executes.put(regPattern, function) ;
		return this ;
	}
	
	public CrakenManager register(String regPattern, QueryExecuteFunction function) {
		register(regPattern, (QueryFunction)function) ;
		register(regPattern, (ExecuteFunction)function) ;
		return this ;
	}

	
	public QueryFunction findQueryFunction(String psql) {
		for(String regEx : querys.keySet()){
			if (psql.matches(regEx)){
				return querys.get(regEx);
			}
		}
		throw new IllegalArgumentException("not found query : " + psql) ;
	}
	public ExecuteFunction findExecuteFunction(String psql) {
		for(String regEx : executes.keySet()){
			if (psql.matches(regEx)){
				return executes.get(regEx);
			}
		}
		throw new IllegalArgumentException("not found query : " + psql) ;
	}

}
