package net.ion.craken.node.convert.rows.function;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.ObjectUtil;

public class DivideFunction extends SingleColumn{
	
	private List<IColumn> columns = ListUtil.newList();
	private String label;
	
	public DivideFunction(ColumnParser cparser, String[] args, String label) {
		for(String arg : args){
			columns.add(cparser.parse(arg.trim()));
		}
		this.label = label;
	}

	public Object getValue(ReadNode node) {
//		BigDecimal d = new BigDecimal(NumberUtil.toDouble(ObjectUtil.toString(columns.get(0).getValue(node)), 0D));
//		return d.divide(divisor);
		double dividen = getDoubleValue(node, columns.get(0).getValue(node));
		for(int i=1; i<columns.size(); i++){
			double divisor = getDoubleValue(node, columns.get(i).getValue(node));
			dividen = dividen / divisor;
		}
		return Double.valueOf(dividen).intValue();
	}
		
	private double getDoubleValue(ReadNode node, Object value) {
		return NumberUtil.toDouble(ObjectUtil.toString(columns.get(0).getValue(node)), 0D);
	}

	public String getLabel() {
		return label;
	}
	

}
