package net.ion.craken.expression;

import net.ion.craken.node.NodeCommon;

public final class BinaryExpression extends ValueObject implements Expression {
	public final Expression left;
	public final Expression right;
	public final Op operator;

	public BinaryExpression(Expression left, Op op, Expression right) {
		this.left = left;
		this.operator = op;
		this.right = right;
	}

	public Comparable value(NodeCommon node) {

		return operator.compute(left.value(node), right.value(node)) ;	
	}
}