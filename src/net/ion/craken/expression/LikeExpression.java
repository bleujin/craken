package net.ion.craken.expression;

import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;

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

	@Override
	public Comparable value(NodeCommon node) {
		String expValue = ObjectUtil.toString(expression.value(node));
		String patternString = ObjectUtil.toString(pattern.value(node)) ;
		
		if (patternString.startsWith("%")){
			return StringUtil.contains(expValue, StringUtil.remove(patternString, '%')) ;
		} else if (patternString.endsWith("%")) {
			return StringUtil.startsWith(expValue, StringUtil.remove(patternString, '%')) ;
		} else {
			return StringUtil.equals(expValue, patternString) ;
		}
	}



}

