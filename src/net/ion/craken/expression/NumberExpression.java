package net.ion.craken.expression;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.NumberUtil;

public final class NumberExpression extends ValueObject implements Expression {
	public final String number;

	public NumberExpression(String number) {
		this.number = number;
	}

	@Override
	public Comparable value(NodeCommon node) {
		return NumberUtil.createBigDecimal(number);
	}
}
