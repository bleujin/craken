package net.ion.rosetta.bleujin.expression;

public final class StringExpression extends ValueObject implements Expression {
	public final String string;

	public StringExpression(String string) {
		this.string = string;
	}
}
