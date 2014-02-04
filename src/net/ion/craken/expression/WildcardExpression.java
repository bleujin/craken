package net.ion.craken.expression;

import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.SetUtil;

public final class WildcardExpression extends ValueObject implements Expression {
	public final QualifiedName owner;

	public WildcardExpression(QualifiedName owner) {
		this.owner = owner;
	}

	@Override
	public Comparable value(NodeCommon node) {
		ComparableSet set = new ComparableSet() ;
		
		return null;
	}
	

}

