package net.ion.craken.expression;

import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.NodeCommonMap;
import net.ion.framework.util.SetUtil;

public final class BetweenExpression extends ValueObject implements Expression {
	public final Expression expression;
	public final boolean between; // between or not between
	public final Expression from;
	public final Expression to;

	public BetweenExpression(Expression expression, boolean between, Expression from, Expression to) {
		this.expression = expression;
		this.between = between;
		this.from = from;
		this.to = to;
	}
	
	public Comparable value(NodeCommonMap node) {
		return (Boolean)(Op.GE.compute(expression.value(node), from.value(node))) && (Boolean)(Op.LE.compute(expression.value(node), to.value(node))) ; 
	}
	
}

