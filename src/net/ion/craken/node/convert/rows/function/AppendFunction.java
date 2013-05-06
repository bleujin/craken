package net.ion.craken.node.convert.rows.function;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.ListUtil;

public class AppendFunction extends SingleColumn {

	private List<IColumn> columns = ListUtil.newList();
	private String label;
	
	public AppendFunction(ColumnParser cparser, String[] args, String label) {
		for(String arg : args){
			columns.add(cparser.parse(arg.trim()));
		}
		this.label = label;
	}
	
	public Object getValue(ReadNode node) {
		StringBuffer result = new StringBuffer();
		for(IColumn col : columns){
			result.append(col.getValue(node));
		}
		return result.toString();
	}

	public String getLabel() {
		return label;
	}

}
