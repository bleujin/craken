package net.ion.craken.expression;

import net.ion.craken.node.NodeCommonMap;
import net.ion.craken.node.crud.Filters;

import org.apache.lucene.search.Filter;

public class BooleanExpression extends ValueObject implements Expression , ConstantExpression{
	public final Boolean bvalue;

	public BooleanExpression(boolean bvalue) {
		this.bvalue = bvalue;
	}

	@Override
	public Comparable value(NodeCommonMap node) {
		return bvalue;
	}
	
	public Object constantValue(){
		return bvalue ;
	}
	
	@Override
	public Filter filter(Op operand, QualifiedNameExpression qne) {
		String field = qne.lastName();
		if( operand == Op.EQ){
			return Filters.eq(qne.lastName(), bvalue.toString()) ;
		} else if (operand == Op.CONTAIN) {
			return Filters.eq(qne.lastName(), bvalue.toString()) ;
		} else if (operand == Op.GT){
			return Filters.gt(field, bvalue.toString()) ;
		} else if (operand == Op.GE) {
			return Filters.gte(field, bvalue.toString()) ;
		} else if (operand == Op.LT) {
			return Filters.lt(field, bvalue.toString()) ;
		} else if (operand == Op.LE){
			return Filters.lte(field, bvalue.toString()) ;
		} else {
			throw new IllegalArgumentException("operand :" + operand) ;
		}
	}
}
