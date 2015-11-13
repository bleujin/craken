package net.ion.craken.node.convert.rows;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Iterator;

import javax.sql.RowSetMetaData;

import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.SelectProjection;
import net.ion.craken.expression.TerminalParser;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.framework.db.RepositoryException;
import net.ion.framework.db.Rows;
import net.ion.framework.db.RowsImpl;
import net.ion.framework.db.procedure.Queryable;
import net.ion.framework.util.StringUtil;
import net.ion.rosetta.Parser;

public class AdNodeRows extends RowsImpl {

	private static final long serialVersionUID = 5559980355928579198L;

	private AdNodeRows(ReadSession session) throws SQLException {
		super(Queryable.Fake);
	}


	

	public static Rows create(ReadSession session, Iterator<ReadNode> iter, String expr, FieldDefinition[] fds, int screenCount, String scLabel)  {
		try {
			SelectProjection projection = makeSelectProjection(expr) ;
			
			FieldContext fcontext = new FieldContext() ;
			projection.add(fcontext, fds) ;

			AdNodeRows result = new AdNodeRows(session);
			result.populate(session, iter, projection, screenCount, scLabel);
			result.beforeFirst();
			return result;
		} catch (SQLException e) {
			throw RepositoryException.throwIt(e) ;
		}
	}

	private void populate(ReadSession session, Iterator<ReadNode> cursor, SelectProjection projection, int screenCount, String scLabel) throws SQLException {
		int columnSize = projection.size() + (StringUtil.isBlank(scLabel) ? 0 : 1 ) ;
		if (cursor.hasNext()) {
			ReadNode firstRow = cursor.next();
			RowSetMetaData meta = makeMetaData(firstRow, projection, screenCount, columnSize, scLabel);
			super.setMetaData(meta);
			appendRow(projection, columnSize, firstRow, screenCount, scLabel);
			while (cursor.hasNext()) {
				appendRow(projection, columnSize, cursor.next(), screenCount, scLabel);
			}
			
		} else {
			super.setMetaData(makeMetaData(null, projection, screenCount, columnSize, scLabel));
		}
	}

	private RowSetMetaData makeMetaData(ReadNode node, SelectProjection projection, int screenCount, int columnSize, String scLabel) throws SQLException {
		RowSetMetaData result = projection.getMetaType(node, columnSize);
		
//		result.setColumnCount(result.getColumnCount() + 1) ;
		if (StringUtil.isBlank(scLabel)) return result ;
		int cindex = result.getColumnCount();
		result.setColumnName(cindex, scLabel) ;
		result.setColumnLabel(cindex, scLabel) ;
		result.setColumnType(cindex, Types.INTEGER) ;
		
		return result ;
	}

	private void appendRow(SelectProjection projection, int columnSize, ReadNode rnode, int screenSize, String scLabel) throws SQLException {
		super.afterLast();
		super.moveToInsertRow();
		projection.updateObject(this, rnode);
		
		if (StringUtil.isNotBlank(scLabel)) super.updateObject(columnSize, screenSize) ;
		super.insertRow();
		super.moveToCurrentRow();

	}

	
	
	
	
	
	
	public static Rows create(ReadSession session, Iterator<ReadNode> iter, String expr, FieldDefinition... fds) {
		try {
			SelectProjection projection = makeSelectProjection(expr);
			
			FieldContext fcontext = new FieldContext() ;
			projection.add(fcontext, fds) ;
			
			AdNodeRows result = new AdNodeRows(session);
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
			appendRow(this, projection, firstRow);
			while (cursor.hasNext()) {
				appendRow(this, projection, cursor.next());
			}
		} else {
			setMetaData(makeMetaData(null, projection));
		}
	}
	
	private RowSetMetaData makeMetaData(ReadNode node, SelectProjection projection) throws SQLException {
		return projection.getMetaType(node, projection.size());
	}

	private static Parser<SelectProjection> parser = ExpressionParser.selectProjection();
	public static SelectProjection makeSelectProjection(String expr) {
		SelectProjection sp = TerminalParser.parse(parser, expr);
		return sp;
	}
	
	public static void appendRow(AdNodeRows rows, SelectProjection projection, ReadNode rnode) throws SQLException {
		rows.afterLast();
		rows.moveToInsertRow();
		projection.updateObject(rows, rnode) ;

		rows.insertRow();
		rows.moveToCurrentRow();
	}
	
	public AdNodeRows unionAll(IteratorList<ReadNode> cursor, String expr) throws SQLException {
		SelectProjection projection = AdNodeRows.makeSelectProjection(expr) ;
		while (cursor.hasNext()) {
			appendRow(this, projection, cursor.next());
		}
		beforeFirst(); 
		return this;
	}

}
