package net.ion.craken.db;

import java.sql.SQLException;

import net.ion.craken.node.ReadSession;
import net.ion.framework.db.Rows;
import net.ion.framework.db.procedure.IParameterQueryable;

public interface QueryFunction {

	public Rows query(ReadSession session, QueryParam query) throws SQLException ; 

}
