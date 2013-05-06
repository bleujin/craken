package net.ion.craken.node.crud;

import java.sql.SQLException;
import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.CrakenNodeRows;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

public class ReadChildren extends IteratorList<ReadNode>{

	private ReadSession session ;
	private Iterator<TreeNode<PropertyId, PropertyValue>> iter ;
	public ReadChildren(ReadSession session, Iterator<TreeNode<PropertyId, PropertyValue>> iter){
		this.session = session ;
		this.iter = iter ;
	}
	
	@Override
	public boolean hasNext() {
		return iter.hasNext();
	}

	@Override
	public ReadNode next() {
		return ReadNodeImpl.load(session, iter.next());
	}

	public List<ReadNode> toList(){
		List<ReadNode> result = ListUtil.newList() ;
		while(hasNext()){
			result.add(next()) ;
		}
		return result ;
	}
	
	public Rows toRows(String... cols) throws SQLException{
		ColumnParser cparser = session.getWorkspace().getAttribute(ColumnParser.class.getCanonicalName(), ColumnParser.class);
		return CrakenNodeRows.create(session, this, cparser.parse(cols));
	}

	public void debugPrint() {
		while(hasNext()){
			Debug.debug(next()) ;
		}
	}
}
