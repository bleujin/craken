package net.ion.rosetta.bleujin.expression;

public final class LikeExpression extends ValueObject implements Expression {
	public final Expression expression;
	public final boolean like; // like or not like
	public final Expression pattern;
	public final Expression escape;

	public LikeExpression(Expression expression, boolean like, Expression pattern, Expression escape) {
		this.expression = expression;
		this.like = like;
		this.pattern = pattern;
		this.escape = escape;
	}
}

