package net.ion.craken.expression;

import net.ion.craken.node.NodeCommon;

public final class StringExpression extends ValueObject implements Expression {
	public final String string;

	public StringExpression(String string) {
		this.string = string;
	}

	@Override
	public Comparable value(NodeCommon node) {
		return string;
	}
}
