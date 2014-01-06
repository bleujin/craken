package net.ion.craken.node.convert.rows.function;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.NumberUtil;

public class TonumberFunction extends SingleColumn {

	private IColumn col ;
	private String label;
	
	public TonumberFunction(ColumnParser cparser, String[] cols, String label) {
		this.col = cparser.parse(cols[0]) ;
		this.label = label;
	}

	public Object getValue(ReadNode node) {
		return NumberUtil.toIntWithMark(col.getValue(node), 0);
	}

	public String getLabel() {
		return label;
	}

}
