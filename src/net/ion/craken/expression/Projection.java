package net.ion.craken.expression;

public final class Projection extends ValueObject {
	public final Expression expression;
	public final String alias;

	public Projection(Expression expression, String alias) {
		this.expression = expression;
		this.alias = alias;
	}
}
