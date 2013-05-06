package net.ion.craken.node.convert.rows;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import javax.sql.RowSetMetaData;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.search.ReadSearchSession;
import net.ion.framework.db.Rows;
import net.ion.framework.db.RowsImpl;
import net.ion.framework.db.procedure.Queryable;
import net.ion.framework.util.ListUtil;

public class CrakenNodeRows extends RowsImpl {

	public CrakenNodeRows(ReadSession session, Iterator<ReadNode> iterator) throws SQLException {
		super(Queryable.Fake);
	}

	public static Rows create(ReadSession session, IteratorList<ReadNode> iter, NodeColumns columns) throws SQLException {
		final CrakenNodeRows result = new CrakenNodeRows(session, iter);
		result.populate(session, iter, columns);
		result.beforeFirst();
		return result;
	}

	private void populate(ReadSession session, Iterator<ReadNode> cursor, NodeColumns columns) throws SQLException {
		if (cursor.hasNext()) {
			ReadNode firstRow = cursor.next();
			RowSetMetaData meta = makeMetaData(firstRow, columns);
			setMetaData(meta);
			appendRow(columns, firstRow, 0);
			while (cursor.hasNext()) {
				appendRow(columns, cursor.next(), 0);
			}
		} else {
			setMetaData(ListUtil.EMPTY, columns);
		}
	}

	private void setMetaData(List<ReadNode> destList, NodeColumns columns) throws SQLException {
		setMetaData(columns.getMetaType(destList));
	}

	private RowSetMetaData makeMetaData(ReadNode node, NodeColumns columns) throws SQLException {
		return columns.getMetaType(ListUtil.create(node));
	}

	private void appendRow(NodeColumns columns, ReadNode firstRow, int screenSize) throws SQLException {
		super.afterLast();
		super.moveToInsertRow();
		for (int i = 1; i <= columns.size(); i++) {
			final IColumn column = columns.get(i);
			super.updateObject(i, column.getValue(firstRow));
		}

		super.insertRow();
		super.moveToCurrentRow();
	}

}
