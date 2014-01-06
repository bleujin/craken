package net.ion.craken.expression;

import net.ion.craken.node.NodeCommon;

public final class UnaryExpression extends ValueObject implements Expression {
	public final Expression operand;
	public final Op operator;

	public UnaryExpression(Op operator, Expression operand) {
		this.operand = operand;
		this.operator = operator;
	}

	@Override
	public Comparable value(NodeCommon node) {
		return operator.compute(operand.value(node));
	}
}