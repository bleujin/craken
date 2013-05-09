package net.ion.craken.node.convert.rows;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.rows.function.SingleColumn;

public class ConstantColumn extends SingleColumn {

	private Object con;
	private String label;

	public ConstantColumn(Object con, String label) {
		this.con = con;
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public Object getValue(ReadNode node) {
		return con;
	}

}
