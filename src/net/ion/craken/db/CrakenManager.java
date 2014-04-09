package net.ion.craken.db;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;

import net.ion.craken.node.ReadSession;
import net.ion.framework.db.IDBController;
import net.ion.framework.db.Rows;
import net.ion.framework.db.manager.DBManager;
import net.ion.framework.db.procedure.RepositoryService;
import net.sf.cglib.proxy.Enhancer;

public abstract class CrakenManager extends DBManager {

	private Connection fake;
	private RepositoryService cservice;

	protected CrakenManager() {
		this.cservice = new CrakenRepositoryService(this);
	}
	
	public abstract Rows queryBy(CrakenUserProcedure crakenUserProcedure) throws Exception;

	public abstract int updateWith(CrakenUserProcedure crakenUserProcedure) throws Exception;

	public abstract int updateWith(CrakenUserProcedureBatch crakenUserProcedureBatch) throws Exception;

	

	@Override
	public Connection getConnection() throws SQLException {
		return this.fake;
	}

	@Override
	public int getDBManagerType() {
		return 77;
	}

	@Override
	public String getDBType() {
		return "crakenFn";
	}

	@Override
	public RepositoryService getRepositoryService() {
		return cservice;
	}

	@Override
	protected void myDestroyPool() throws Exception {

	}

	protected void heartbeatQuery(IDBController dc) throws SQLException {
		// no action
	}

	@Override
	protected void myInitPool() throws SQLException {
		Enhancer e = new Enhancer();
		e.setSuperclass(Connection.class);
		e.setCallback(new ConnectionMock());

		this.fake = (Connection) e.create();
	}
	
	
}
