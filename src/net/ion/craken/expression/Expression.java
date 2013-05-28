package net.ion.craken.expression;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;

public interface Expression {
	public Comparable value(NodeCommon node) ;
}

