package net.ion.craken.node.convert.rows.function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.StringUtil;


public class LengthFunction extends SingleColumn{
	
	private IColumn col ;
	private String label;
	
	public LengthFunction(ColumnParser cparser, String[] args, String label) {
		col = cparser.parse(args[0]) ;
		this.label = label ;
	}
	
	public String getLabel() {
		return label;
	}

	public Object getValue(ReadNode node) {
		String val = String.valueOf(col.getValue(node));
		return StringUtil.length(val);
	}

}
