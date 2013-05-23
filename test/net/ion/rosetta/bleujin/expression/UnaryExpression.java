package net.ion.rosetta.bleujin.expression;

public final class UnaryExpression extends ValueObject implements Expression {
	public final Expression operand;
	public final Op operator;

	public UnaryExpression(Op operator, Expression operand) {
		this.operand = operand;
		this.operator = operator;
	}
}