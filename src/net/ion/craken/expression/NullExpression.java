package net.ion.craken.expression;

import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.SetUtil;

public final class NullExpression implements Expression {
	private NullExpression() {
	}

	public static final NullExpression instance = new NullExpression();

	@Override
	public Comparable value(NodeCommon node) {
		return null;
	}


}
