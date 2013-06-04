package net.ion.craken.node.convert.rows;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;

import javax.sql.RowSetMetaData;

import net.ion.craken.expression.SelectProjection;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.framework.db.RepositoryException;
import net.ion.framework.db.Rows;
import net.ion.framework.db.RowsImpl;
import net.ion.framework.db.procedure.Queryable;

public class AdNodeRows extends RowsImpl {

	private static final long serialVersionUID = 5559980355928579198L;

	private AdNodeRows(ReadSession session, Iterator<ReadNode> iterator) throws SQLException {
		super(Queryable.Fake);
	}


	

	public static Rows create(ReadSession session, Iterator<ReadNode> iter, SelectProjection projection, int screenCount, String scLabel)  {
		try {
			AdNodeRows result = new AdNodeRows(session, iter);
			result.populate(session, iter, projection, screenCount, scLabel);
			result.beforeFirst();
			return result;
		} catch (SQLException e) {
			throw RepositoryException.throwIt(e) ;
		}
	}

	private void populate(ReadSession session, Iterator<ReadNode> cursor, SelectProjection projection, int screenCount, String scLabel) throws SQLException {
		if (cursor.hasNext()) {
			ReadNode firstRow = cursor.next();
			RowSetMetaData meta = makeMetaData(firstRow, projection, screenCount, scLabel);
			super.setMetaData(meta);
			appendRow(projection, firstRow, screenCount);
			while (cursor.hasNext()) {
				appendRow(projection, cursor.next(), screenCount);
			}
		} else {
			super.setMetaData(makeMetaData(null, projection, screenCount, scLabel));
		}
	}

	private RowSetMetaData makeMetaData(ReadNode node, SelectProjection projection, int screenCount, String scLabel) throws SQLException {
		RowSetMetaData result = projection.getMetaType(node, projection.size() + 1);
		
//		result.setColumnCount(result.getColumnCount() + 1) ;
		int cindex = result.getColumnCount();
		result.setColumnName(cindex, scLabel) ;
		result.setColumnLabel(cindex, scLabel) ;
		result.setColumnType(cindex, Types.INTEGER) ;
		
		return result ;
	}

	private void appendRow(SelectProjection projection, ReadNode rnode, int screenSize) throws SQLException {
		super.afterLast();
		super.moveToInsertRow();
		projection.updateObject(this, rnode);
		
		super.updateObject(projection.size() +1, screenSize) ;

		super.insertRow();
		super.moveToCurrentRow();
	}

	
	
	
	
	
	
	public static Rows create(ReadSession session, Iterator<ReadNode> iter, SelectProjection projection) {
		try {
			AdNodeRows result = new AdNodeRows(session, iter);
			result.populate(session, iter, projection);
			result.beforeFirst();
			return result;
		} catch (SQLException e) {
			throw RepositoryException.throwIt(e) ;
		}
	}

	private void populate(ReadSession session, Iterator<ReadNode> cursor, SelectProjection projection) throws SQLException {
		if (cursor.hasNext()) {
			ReadNode firstRow = cursor.next();
			RowSetMetaData meta = makeMetaData(firstRow, projection);
			setMetaData(meta);
			appendRow(projection, firstRow);
			while (cursor.hasNext()) {
				appendRow(projection, cursor.next());
			}
		} else {
			setMetaData(makeMetaData(null, projection));
		}
	}
	
	private RowSetMetaData makeMetaData(ReadNode node, SelectProjection projection) throws SQLException {
		return projection.getMetaType(node, projection.size());
	}

	private void appendRow(SelectProjection projection, ReadNode rnode) throws SQLException {
		super.afterLast();
		super.moveToInsertRow();
		projection.updateObject(this, rnode) ;

		super.insertRow();
		super.moveToCurrentRow();
	}
}
