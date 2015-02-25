package net.ion.craken.expression;

import net.ion.craken.node.NodeCommonMap;

public final class WildcardExpression extends ValueObject implements Expression {
	public final QualifiedName owner;

	public WildcardExpression(QualifiedName owner) {
		this.owner = owner;
	}

	@Override
	public Comparable value(NodeCommonMap node) {
		ComparableSet set = new ComparableSet() ;
		
		return null;
	}
	

}

