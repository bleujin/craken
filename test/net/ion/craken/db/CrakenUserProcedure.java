package net.ion.craken.db;

import java.io.InputStream;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.concurrent.ExecutionException;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.CrakenNodeRows;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.Page;
import net.ion.framework.db.Rows;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.db.procedure.IParameterQueryable;
import net.ion.framework.db.procedure.IQueryable;
import net.ion.framework.db.procedure.IUserProcedure;
import net.ion.framework.db.procedure.IUserProcedureBatch;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;

public class CrakenUserProcedure implements IUserProcedure, QueryParam {


	private IDBController dc ;
	private IUserProcedure inner ;
	private CrakenManager manager;
	public CrakenUserProcedure(IDBController dc, CrakenManager manager, IUserProcedure inner) {
		this.dc = dc ;
		this.manager = manager ;
		this.inner = inner ;
	}

	@Override
	public String getProcName() {
		return inner.getProcName();
	}

	@Override
	public IParameterQueryable addBlob(InputStream input) {
		return inner.addBlob(input);
	}

	@Override
	public void addBlob(int index, InputStream input) {
		inner.addBlob(index, input) ;
	}

	@Override
	public IParameterQueryable addBlob(String name, InputStream value) {
		inner.addBlob(name, value) ;
		return this;
	}

	@Override
	public IParameterQueryable addClob(CharSequence value) {
		inner.addClob(value) ;
		return this;
	}

	@Override
	public IParameterQueryable addClob(Reader value) {
		inner.addClob(value) ;
		return this;
	}

	@Override
	public void addClob(int index, CharSequence value) {
		inner.addClob(index, value) ;
	}

	@Override
	public void addClob(int index, Reader value) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public IParameterQueryable addClob(String arg0, CharSequence arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IParameterQueryable addClob(String arg0, Reader arg1) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public IParameterQueryable addParam(boolean value) {
		inner.addParam(value) ;
		return this;
	}

	@Override
	public IParameterQueryable addParam(int value) {
		inner.addParam(value) ;
		return this;
	}

	@Override
	public IParameterQueryable addParam(long value) {
		inner.addParam(value) ;
		return this;
	}

	@Override
	public IParameterQueryable addParam(CharSequence value) {
		inner.addParam(value) ;
		return this;
	}

	@Override
	public IParameterQueryable addParam(Object value) {
		inner.addParam(value) ;
		return this;
	}

	@Override
	public void addParam(int index, boolean value) {
		inner.addParam(index, value) ;
	}

	@Override
	public void addParam(int index, int value) {
		inner.addParam(index, value) ;
	}

	@Override
	public void addParam(int index, long value) {
		inner.addParam(index, value) ;
	}

	@Override
	public void addParam(int index, CharSequence value) {
		inner.addParam(index, value) ;
	}

	@Override
	public void addParam(int index, Object value) {
		inner.addParam(index, value) ;
	}

	@Override
	public IParameterQueryable addParam(String name, boolean value) {
		inner.addParam(name, value) ;
		return this;
	}

	@Override
	public IParameterQueryable addParam(String name, int value) {
		inner.addParam(name, value) ;
		return null;
	}

	@Override
	public IParameterQueryable addParam(String name, long value) {
		inner.addParam(name, value) ;
		return this;
	}

	@Override
	public IParameterQueryable addParam(String name, CharSequence value) {
		inner.addParam(name, value) ;
		return this;
	}

	@Override
	public IParameterQueryable addParam(String name, Object value) {
		inner.addParam(name, value) ;
		return this;
	}

	@Override
	public IParameterQueryable addParameter(Object name, int value) {
		inner.addParameter(name, value) ;
		return this;
	}

	@Override
	public void addParameter(int paramindex, Object param, int type) {
		inner.addParameter(paramindex, param, type) ;
	}

	@Override
	public IParameterQueryable addParameter(String name, Object param, int type) {
		inner.addParameter(name, param, type) ;
		return this;
	}

	@Override
	public void clearParam() {
		inner.clearParam() ;
	}

	@Override
	public String getParamAsString(int index) {
		return inner.getParamAsString(index);
	}

	@Override
	public List getParams() {
		return inner.getParams();
	}

	@Override
	public String[] getParamsAsString(int index) {
		return inner.getParamsAsString(index);
	}

	@Override
	public String getProcFullSQL() {
		return inner.getProcFullSQL();
	}

	@Override
	public int getType(int index) {
		return inner.getType(index);
	}

	@Override
	public Object getParam(int index) {
		return getParams().get(index);
	}

	@Override
	public int getParamType(int index) {
		return getType(index) ;
	}

	
	@Override
	public List getTypes() {
		return inner.getTypes();
	}

	@Override
	public boolean isNull(int paramIndex) {
		return inner.isNull(paramIndex);
	}

	@Override
	public void setParamValues(List arg0, List values) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public long getCurrentModifyCount() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public IDBController getDBController() {
		return dc;
	}

	@Override
	public Statement getStatement() throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T myHandlerQuery(Connection arg0, ResultSetHandler<T> arg1) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Rows myQuery(Connection arg0) throws SQLException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int myUpdate(Connection arg0) throws SQLException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void cancel() throws SQLException, InterruptedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getDBType() {
		return manager.getDBType();
	}

	
	
	
	
	@Override
	public Page getPage() {
		return inner.getPage();
	}

	@Override
	public String getProcSQL() {
		return inner.getProcSQL() ;
	}

	@Override
	public int getQueryType() {
		return inner.getQueryType();
	}

	@Override
	public IQueryable setPage(Page page) {
		inner.setPage(page) ;
		return this;
	}



	
	
	@Override
	public <T> T execHandlerQuery(ResultSetHandler<T> handler) throws SQLException {
		return execQuery().toHandle(handler);
	}
	
	@Override
	public Rows execPageQuery() throws SQLException {
		QueryFunction function = manager.findQueryFunction(getProcName()) ;
		return function.query(manager.session(), this) ;
	}

	@Override
	public Rows execQuery() throws SQLException {
		long start = 0, end = 0;
		Rows rows = null ;
		try {
			start = System.nanoTime();
			QueryFunction function = manager.findQueryFunction(getProcName()) ;
			rows = function.query(manager.session(), this);
		} finally {
			end = System.nanoTime();
			getDBController().handleServant(start, end, this, IQueryable.QUERY_COMMAND);
		}
		return rows ;
	}

	@Override
	public int execUpdate() throws SQLException  {
		try {
			ExecuteFunction function = manager.findExecuteFunction(getProcName()) ;
			return function.execute(manager.session(), this) ;
		} catch (InterruptedException e) {
			throw new SQLException(e) ;
		} catch (ExecutionException e) {
			throw new SQLException(e) ;
		}
	}

	
	
	
	
	// ..
	public String getString(int index){
		return getParamAsString(index) ;
	}
	
	public int getInt(int index){
		Object obj = getParam(index) ;
		if (obj == null) {
			return 0 ;
		} else if (obj instanceof Integer){
			return ((Integer)obj).intValue() ;
		} else if (obj instanceof Long){
			return ((Long)obj).intValue() ;
		} else {
			return NumberUtil.toInt(ObjectUtil.toString(obj), 0) ;
		}
	}

	@Override
	public String procName() {
		return this.getProcName() ;
	}


	
}
