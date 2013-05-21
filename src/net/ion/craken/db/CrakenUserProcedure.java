package net.ion.craken.db;

import java.io.InputStream;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
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
import net.ion.framework.db.procedure.UserProcedure;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;

public class CrakenUserProcedure extends UserProcedure implements QueryParam {


	private IDBController dc ;
	private CrakenManager manager;
	public CrakenUserProcedure(IDBController dc, CrakenManager manager, String psql) {
		super(dc, psql);
		this.dc = dc ;
		this.manager = manager ;
	}

	


	@Override
	public Statement getStatement() throws SQLException {
		throw new UnsupportedOperationException() ;
	}


	@Override
	public <T> T myHandlerQuery(Connection conn, ResultSetHandler<T> handler) throws SQLException {
		return myQuery(conn).toHandle(handler); // only client handler
	}



	@Override
	public Rows myQuery(Connection conn) throws SQLException {
		try {
			return manager.queryBy(this) ;
		} catch (IllegalArgumentException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} catch (IllegalAccessException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} catch (InvocationTargetException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} catch (NoSuchMethodException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} catch (SecurityException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} catch (NoSuchFieldException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} 	
	}





	@Override
	public int myUpdate(Connection conn) throws SQLException {
		try {
			return manager.updateWith(this) ;
		} catch (IllegalArgumentException e) {
			throw new SQLException(e) ;
		} catch (IllegalAccessException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} catch (InvocationTargetException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} catch (NoSuchMethodException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} catch (SecurityException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} catch (NoSuchFieldException e) {
			throw new SQLException(ObjectUtil.coalesce(e.getCause(), e)) ;
		} 
	}


	
	
	// ..
	public String getString(int index){
		return getParamAsString(index) ;
	}
	
	public int getInt(int index){
		Object obj = getParams().get(index) ;
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
