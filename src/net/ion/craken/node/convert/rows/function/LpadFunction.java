package net.ion.craken.node.convert.rows.function;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;

public class LpadFunction extends SingleColumn {
	
	private List<IColumn> columns = ListUtil.newList();
	private String label;
	
	public LpadFunction(ColumnParser cparser, String[] args, String label) {
		for(String arg : args){
			columns.add(cparser.parse(arg.trim()));
		}
		this.label = label;
	}
	
	public Object getValue(ReadNode node) {
		if(columns.size() == 2){
			return StringUtil.leftPad(ObjectUtil.toString(columns.get(0).getValue(node)), NumberUtil.toIntWithMark(columns.get(1).getValue(node), 0));
		}else if(columns.size() == 3){
			return StringUtil.leftPad(ObjectUtil.toString(columns.get(0).getValue(node)), NumberUtil.toIntWithMark(columns.get(1).getValue(node), 0), 
									ObjectUtil.toString(columns.get(2).getValue(node)));
		}else{
			return "";
		}
	}

	public String getLabel() {
		return label;
	}

}
