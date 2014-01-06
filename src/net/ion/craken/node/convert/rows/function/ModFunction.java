package net.ion.craken.node.convert.rows.function;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.NumberUtil;

public class ModFunction extends SingleColumn {

	private List<IColumn> columns = ListUtil.newList();
	private String label;

	public ModFunction(ColumnParser cparser, String[] cols, String label) {
		for (String col : cols) {
			columns.add(cparser.parse(col.trim()));
		}
		this.label = label;
	}

	public Object getValue(ReadNode node) {
		return NumberUtil.toIntWithMark(columns.get(0).getValue(node), 0) % NumberUtil.toIntWithMark(columns.get(1).getValue(node), 0);
	}

	public String getLabel() {
		return label;
	}

}
