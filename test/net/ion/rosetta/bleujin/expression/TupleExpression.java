package net.ion.rosetta.bleujin.expression;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Models a tuple of expressions such as "(1, 2, 3)".
 * 
 * @author Ben Yu
 */
public class TupleExpression extends ValueObject implements Expression {
	public final List<Expression> expressions;

	public TupleExpression(List<Expression> expressions) {
		this.expressions = Collections.unmodifiableList(expressions);
	}

	public static TupleExpression of(Expression... expressions) {
		return new TupleExpression(Arrays.asList(expressions));
	}
}