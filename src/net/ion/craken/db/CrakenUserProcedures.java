package net.ion.craken.db;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import net.ion.framework.db.IDBController;
import net.ion.framework.db.Page;
import net.ion.framework.db.Rows;
import net.ion.framework.db.bean.ResultSetHandler;
import net.ion.framework.db.procedure.IQueryable;
import net.ion.framework.db.procedure.IUserProcedures;
import net.ion.framework.db.procedure.Queryable;
import net.ion.framework.db.procedure.UserProcedures;

public class CrakenUserProcedures extends UserProcedures {

	private static final long serialVersionUID = -4130641335578767179L;
	private CrakenManager cm;
	public CrakenUserProcedures(IDBController dc, CrakenManager cm, String name) {
		super(dc, name);
		this.cm = cm ;
	}

	
	public int myUpdate(Connection conn) throws SQLException {
		try {
			return cm.updateWith(this) ;
		} catch (Exception e) {
			throw new SQLException(e) ;
		}
		
	}
}
