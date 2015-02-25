package net.ion.craken.expression;

import net.ion.craken.node.NodeCommonMap;

public final class NullExpression implements Expression {
	private NullExpression() {
	}

	public static final NullExpression instance = new NullExpression();

	@Override
	public Comparable value(NodeCommonMap node) {
		return null;
	}


}
