package net.ion.craken.node.convert.rows.function;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.ObjectUtil;

import org.apache.commons.lang.ArrayUtils;

public class DecodeFunction extends SingleColumn {

	private List<IColumn> columns = ListUtil.newList();
	private String label;

	public DecodeFunction(ColumnParser cparser, String[] cols, String label) {
		for (int i = 0; i < cols.length; i++) {
			columns.add(cparser.parse(cols[i].trim()));
		}
		this.label = label;
	}


	public String getLabel() {
		return label;
	}

	public Object getValue(ReadNode node) {
		return recursiveDecode(node, columns.toArray(new IColumn[0])) ;
	}
	
	private Object recursiveDecode(ReadNode node, IColumn[] args){
		if (args.length < 3) throw new IllegalArgumentException("not permitted") ;
		if (args.length == 3) return ObjectUtil.equals(args[0].getValue(node), args[1].getValue(node)) ? args[2].getValue(node) : null ;
		if (args.length == 4) return ObjectUtil.equals(args[0].getValue(node), args[1].getValue(node)) ? args[2].getValue(node) : args[3].getValue(node);
		else {
			if (ObjectUtil.equals(args[0].getValue(node), args[1].getValue(node))) {
				return args[2].getValue(node)  ;
			} else {
				List<IColumn> newList = new ArrayList(Arrays.asList(ArrayUtils.subarray(args, 3, args.length)));
				newList.add(0, args[0]) ;
				return recursiveDecode(node, newList.toArray(new IColumn[0])) ;
			}
		}
	}
	
}
