package net.ion.craken.node.convert.rows.function;

import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.IColumn;
import net.ion.framework.util.ListUtil;


public class NvlFunction extends SingleColumn {

	private List<IColumn> columns = ListUtil.newList();
	private String label;

	public NvlFunction(ColumnParser cparser, String[] cols, String label) {
		for (int i = 0; i < cols.length; i++) {
			columns.add(cparser.parse(cols[i].trim()));
		}

		this.label = label;
	}


	public String getLabel() {
		return label;
	}

	public Object getValue(ReadNode node) {
		for (IColumn col : columns) {
			Object result = col.getValue(node);
			if (result != null)
				return result;
		}
		return null;
	}

}
