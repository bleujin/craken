package net.ion.craken.node.convert;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.CrakenNodeRows;
import net.ion.framework.db.Rows;
import net.ion.framework.util.ListUtil;

import com.google.common.base.Function;

public class Functions {

	public final static Function<ReadNode, Rows> rowsFunction(final ReadSession session, final String... cols){
		return new Function<ReadNode, Rows>(){
			@Override
			public Rows apply(ReadNode node) {
				ColumnParser cparser = session.getWorkspace().getAttribute(ColumnParser.class.getCanonicalName(), ColumnParser.class);
				return CrakenNodeRows.create(session, ListUtil.toList(node).iterator() , cparser.parse(cols)) ;
			}
		} ;
	}
	

}
