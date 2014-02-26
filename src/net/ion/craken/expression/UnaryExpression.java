package net.ion.craken.expression;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import org.apache.commons.lang.reflect.MethodUtils;
import org.apache.lucene.search.Filter;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.crud.Filters;
import net.ion.framework.util.SetUtil;

public final class UnaryExpression extends ValueObject implements Expression {
	public final Expression operand;
	public final Op operator;

	public UnaryExpression(Op operator, Expression operand) {
		this.operand = operand;
		this.operator = operator;
	}

	@Override
	public Comparable value(NodeCommon node) {
		return operator.compute(operand.value(node));
	}
	
	
	public Filter filter(){
		try {
			Filter filter = (Filter) MethodUtils.invokeMethod(operand, "filter", new Object[0]);
			return Filters.not(filter) ;
		} catch (NoSuchMethodException e) {
			throw new IllegalArgumentException("can't make filter : " + operand) ;
		} catch (IllegalAccessException e) {
			throw new IllegalArgumentException("can't make filter : " + operand) ;
		} catch (InvocationTargetException e) {
			throw new IllegalArgumentException("can't make filter : " + operand) ;
		}
	}

}