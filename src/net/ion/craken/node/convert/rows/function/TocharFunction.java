package net.ion.craken.node.convert.rows.function;

import java.util.Date;

import net.ion.craken.node.ReadNode;
import net.ion.framework.util.DateUtil;
import net.ion.framework.util.StringUtil;

public class TocharFunction extends SingleColumn {

	private String colName ;
	private String format ;
	private String label;

	public TocharFunction(String[] cols, String label) {
		this.colName = cols[0] ;
		if(cols.length > 1)
			this.format = StringUtil.substringBetween(cols[1], "'") ;
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public Object getValue(ReadNode node) {
		Object value = node.property(colName).value() ;
		if (value instanceof Date){
			return DateUtil.dateToString((Date)value, format) ;
		}else{
			return String.valueOf(value);
		}
		//throw new IllegalArgumentException("not date format");
	}
	
}
