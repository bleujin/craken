package net.ion.rosetta.bleujin.expression;

public final class BinaryExpression extends ValueObject implements Expression {
	public final Expression left;
	public final Expression right;
	public final Op operator;

	public BinaryExpression(Expression left, Op op, Expression right) {
		this.left = left;
		this.operator = op;
		this.right = right;
	}
}