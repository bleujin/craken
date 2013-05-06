package net.ion.craken.node.convert.rows.function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.NumberUtil;

public class SignFunction extends SingleColumn{
	
	private IColumn col ;
	private String label;
	
	public SignFunction(ColumnParser cparser, String[] args, String label) {
		col = cparser.parse(args[0]) ;
		this.label = label ;
	}
	
	public String getLabel() {
		return label;
	}

	public Object getValue(ReadNode node) {
		String val = String.valueOf(col.getValue(node));
		Long result = NumberUtil.toLong( val);
		return result == 0L ? 0 : (result > 0 ? 1 : -1) ; 
	}

}
