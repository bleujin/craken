package net.ion.craken.expression;

import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.crud.Filters;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.SetUtil;

import org.apache.lucene.search.Filter;

public final class StringExpression extends ValueObject implements Expression, ConstantExpression {
	public final String string;

	public StringExpression(String string) {
		this.string = string;
	}

	@Override
	public Comparable value(NodeCommon node) {
		return string;
	}

	public Object constantValue(){
		return string ;
	}
	
	@Override
	public Filter filter(Op operand, QualifiedNameExpression qne) {
		String field = qne.lastName();
		if( operand == Op.EQ){
			return Filters.eq(qne.lastName(), string) ;
		} else if (operand == Op.CONTAIN) {
			return Filters.eq(qne.lastName(), string) ;
		} else if (operand == Op.GT){
			return Filters.gt(field, string) ;
		} else if (operand == Op.GE) {
			return Filters.gte(field, string) ;
		} else if (operand == Op.LT) {
			return Filters.lt(field, string) ;
		} else if (operand == Op.LE){
			return Filters.lte(field, string) ;
		} else {
			throw new IllegalArgumentException("operand :" + operand) ;
		}
	}
	

}
