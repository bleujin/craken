package net.ion.craken.expression;

import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.crud.Filters;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.SetUtil;

import org.apache.lucene.search.Filter;

public final class NumberExpression extends ValueObject implements Expression , ConstantExpression{
	public final String number;

	public NumberExpression(String number) {
		this.number = number;
	}

	@Override
	public Comparable value(NodeCommon node) {
		return NumberUtil.createBigDecimal(number);
	}
	
	
	@Override
	public Filter filter(Op operand, QualifiedNameExpression qne) {
		String field = qne.lastName();
		long longValue = Long.parseLong(number) ;
		if( operand == Op.EQ){
			return Filters.eq(qne.lastName(), longValue) ;
		} else if (operand == Op.CONTAIN) {
			return Filters.eq(qne.lastName(), longValue) ;
		} else if (operand == Op.GT){
			return Filters.gt(field, longValue) ;
		} else if (operand == Op.GE) {
			return Filters.gte(field, longValue) ;
		} else if (operand == Op.LT) {
			return Filters.lt(field, longValue) ;
		} else if (operand == Op.LE){
			return Filters.lte(field, longValue) ;
		} else {
			throw new IllegalArgumentException("operand :" + operand) ;
		}
	}
}
