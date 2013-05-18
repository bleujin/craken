package net.ion.craken.db;

import java.sql.SQLException;
import java.util.concurrent.ExecutionException;

import net.ion.craken.node.ReadSession;
import net.ion.framework.db.Rows;
import net.ion.framework.db.procedure.IParameterQueryable;

public interface ExecuteFunction {
	
	public int execute(ReadSession session, QueryParam query) throws SQLException, InterruptedException, ExecutionException ; 
}
