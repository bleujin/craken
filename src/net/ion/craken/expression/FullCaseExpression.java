package net.ion.craken.expression;
import java.util.Collections;
import java.util.List;

import net.ion.craken.node.NodeCommon;
import net.ion.rosetta.functors.Pair;

public final class FullCaseExpression extends ValueObject implements Expression {
	
	public final List<Pair<Expression, Expression>> cases;
	public final Expression defaultValue;

	public FullCaseExpression(List<Pair<Expression, Expression>> cases, Expression defaultValue) {
		this.cases = Collections.unmodifiableList(cases);
		this.defaultValue = defaultValue;
	}

	@Override
	public Comparable value(NodeCommon node) {
		for (Pair<Expression, Expression> pair : cases) {
			if (Boolean.TRUE.equals(pair.a.value(node))) return pair.b.value(node) ;
		}
		return defaultValue.value(node);
	}
}

