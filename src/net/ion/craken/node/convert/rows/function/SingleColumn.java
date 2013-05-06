package net.ion.craken.node.convert.rows.function;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Map;

import javax.sql.RowSetMetaData;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.IColumn;


public abstract class SingleColumn implements IColumn {

	public int setMeta(ReadNode node, int index, RowSetMetaData meta, Map<Class, Integer> mapping) throws SQLException {

		meta.setColumnName(index, getLabel());
		meta.setColumnLabel(index, getLabel());
		Object value = getValue(node);
		meta.setColumnType(index, value == null ? Types.OTHER : mapping.get(value.getClass()));
		meta.setColumnTypeName(index, value == null ? "other" : value.getClass().getName());
		
		return 0;
	}

	public int getColumnCount(ReadNode node) {
		return 1;
	}

}