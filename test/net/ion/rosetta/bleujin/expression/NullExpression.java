package net.ion.rosetta.bleujin.expression;

public final class NullExpression implements Expression {
	private NullExpression() {
	}

	public static final NullExpression instance = new NullExpression();
}
