package net.ion.craken.expression;

import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.NodeCommonMap;

public interface Expression {
	public Comparable value(NodeCommonMap node) ;
}

