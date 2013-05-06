package net.ion.craken.node.convert.rows;

import java.sql.SQLException;

public interface ColumnParser {

	public NodeColumns parse(String... columns) throws SQLException ;
	
//	public abstract IColumn nvl(String... cols)  ;
//
//	public abstract IColumn constant(Object con, String label)  ;

	public IColumn parse(String expression) ;

}

