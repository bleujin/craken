package net.ion.craken.expression;

import net.ion.craken.node.NodeCommonMap;

public final class RelationNameExpression extends ValueObject implements Expression {
	
	public final RelationName qname;

	public RelationNameExpression(RelationName qname) {
		this.qname = qname;
	}

	public static RelationNameExpression of(String... names) {
		return new RelationNameExpression(RelationName.of(names));
	}

	@Override
	public Comparable value(NodeCommonMap node) {
		return null;
	}

}
