package net.ion.craken.node.convert.rows.function;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.NumberUtil;


public class MinusFunction extends SingleColumn{

	private List<IColumn> columns = ListUtil.newList();
	private String label;
	
	public MinusFunction(ColumnParser cparser, String[] args, String label) {
		for(String arg:args){
			columns.add(cparser.parse(arg.trim()));
		}
		this.label = label;
	}
	
	public Object getValue(ReadNode node) {
		int result =  NumberUtil.toIntWithMark(columns.get(0).getValue(node), 0);
		for(int i=1; i<columns.size(); i++){
			result = result -NumberUtil.toIntWithMark(columns.get(i).getValue(node), 0);
		}
		return result;
	}

	public String getLabel() {
		return label;
	}

}
