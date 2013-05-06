package net.ion.craken.node.convert.rows.function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;


public class FloorFunction  extends SingleColumn{
	
	private IColumn col ;
	private String label;
	
	public FloorFunction(ColumnParser cparser, String[] args, String label) {
		col = cparser.parse(args[0].trim());
		this.label = label;
	}
	
	public Object getValue(ReadNode node) {
		double result = Math.floor(NumberUtil.toDouble(ObjectUtil.toString(col.getValue(node)))); 
		return Double.valueOf(result).intValue();
	}
	public String getLabel() {
		return label;
	}
}
