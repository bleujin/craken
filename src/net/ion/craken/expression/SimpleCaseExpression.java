package net.ion.craken.expression;

import java.util.Collections;
import java.util.List;

import net.ion.craken.node.NodeCommon;
import net.ion.rosetta.functors.Pair;

public final class SimpleCaseExpression extends ValueObject implements Expression {
	public final Expression condition;
	public final List<Pair<Expression, Expression>> cases;
	public final Expression defaultValue; // null if no default

	public SimpleCaseExpression(Expression condition, List<Pair<Expression, Expression>> cases, Expression defaultValue) {
		this.condition = condition;
		this.cases = Collections.unmodifiableList(cases);
		this.defaultValue = defaultValue;
	}

	@Override
	public Comparable value(NodeCommon node) {
		// TODO Auto-generated method stub
		return null;
	}
}
