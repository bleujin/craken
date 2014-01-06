package net.ion.craken.node.convert.rows.function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.StringUtil;

public class SubstrFunction extends SingleColumn{


	private IColumn col ;
	private String label ;
	private IColumn begin ;
	private IColumn end ;
	
	public SubstrFunction(ColumnParser cparser, String[] args, String label){
		col = cparser.parse(args[0]) ;
		begin =  cparser.parse(args[1].trim());
		end = args.length == 3 ?  cparser.parse(args[2].trim()) :  cparser.parse(String.valueOf(Integer.MAX_VALUE)) ;
		
		this.label = label ;
	}
	
	public String getLabel() {
		return label;
	}

	public Object getValue(ReadNode node) {
		String result = StringUtil.toString(col.getValue(node));
		return result.substring( NumberUtil.toIntWithMark(begin.getValue(node), 0) ,  Math.min(NumberUtil.toIntWithMark(end.getValue(node),0), result.length())) ;
	}

}
