package net.ion.craken.node.convert.rows.function;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;

public class PowerFunction extends SingleColumn{

	private List<IColumn> columns = ListUtil.newList();
	private String label;
	
	public PowerFunction(ColumnParser cparser, String[] args, String label) {
		for(String arg : args){
			columns.add(cparser.parse(arg.trim()));
		}
		this.label = label;
	}
	
	public Object getValue(ReadNode node) {
		double result = Math.pow( NumberUtil.toDouble(ObjectUtil.toString(columns.get(0).getValue(node)), 0D), 
					NumberUtil.toDouble(ObjectUtil.toString(columns.get(1).getValue(node)), 0D));
		return Double.valueOf(result).intValue();
	}

	public String getLabel() {
		return label;
	}

}

//private IColumn col ;
//private String label;
//
//public LengthFunction(String[] args, String label) {
//	col = Column.parse(args[0]) ;
//	this.label = label ;
//}
//
//public String getLabel() {
//	return label;
//}
//
//public Object getValue(TreeNode node) {
//	String val = String.valueOf(col.getValue(node));
//	return StringUtil.length(val);
//}