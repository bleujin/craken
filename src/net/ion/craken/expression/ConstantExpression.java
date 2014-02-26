package net.ion.craken.expression;

import org.apache.lucene.search.Filter;

public interface ConstantExpression {
	public Filter filter(Op op, QualifiedNameExpression qne) ; 
	public Object constantValue() ;
}
