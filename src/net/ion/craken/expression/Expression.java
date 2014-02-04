package net.ion.craken.expression;

import java.util.Set;

import net.ion.craken.node.NodeCommon;

public interface Expression {
	public Comparable value(NodeCommon node) ;
}

