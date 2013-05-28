package net.ion.craken.expression;

import java.util.Map.Entry;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.convert.Predicates;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyId.PType;

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

