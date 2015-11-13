package net.ion.craken.node.convert.rows;

import java.sql.SQLException;

import net.ion.craken.expression.SelectProjection;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;

public class AdNodeRowsBuilder {

	private ReadSession session;
	private AdNodeRows rows;

	private AdNodeRowsBuilder(ReadSession session, AdNodeRows rows) {
		this.session = session ;
		this.rows = rows ;
	}

	public static AdNodeRowsBuilder create(ReadSession session, IteratorList<ReadNode> iterator, String expr) {
		AdNodeRows rows = (AdNodeRows) AdNodeRows.create(session, iterator, expr) ;
		
		AdNodeRowsBuilder result = new AdNodeRowsBuilder(session, rows);
		return result;
	}

	public AdNodeRowsBuilder unionAll(IteratorList<ReadNode> cursor, String expr) throws SQLException {
		SelectProjection projection = AdNodeRows.makeSelectProjection(expr) ;
		while (cursor.hasNext()) {
			AdNodeRows.appendRow(rows, projection, cursor.next());
		}
		
		return this;
	}
	public AdNodeRows build() throws SQLException {
		rows.beforeFirst();
		return rows ;
	}

}
