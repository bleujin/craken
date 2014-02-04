package net.ion.craken.expression;

import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.SetUtil;

public final class RelationNameExpression extends ValueObject implements Expression {
	
	public final RelationName qname;

	public RelationNameExpression(RelationName qname) {
		this.qname = qname;
	}

	public static RelationNameExpression of(String... names) {
		return new RelationNameExpression(RelationName.of(names));
	}

	@Override
	public Comparable value(NodeCommon node) {
		// TODO Auto-generated method stub
		return null;
	}

}
