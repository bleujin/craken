package net.ion.craken.node.convert.rows;

import java.sql.SQLException;
import java.util.Map;

import javax.sql.RowSetMetaData;

import net.ion.craken.node.ReadNode;

public interface IColumn {
	public Object getValue(ReadNode node);

	public int getColumnCount(ReadNode node);

	public int setMeta(ReadNode node, int i, RowSetMetaData meta, Map<Class, Integer> typeMappingMap) throws SQLException;

	public String getLabel();
}
