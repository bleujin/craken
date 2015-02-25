package net.ion.craken.expression;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

import net.ion.craken.node.NodeCommonMap;
import net.ion.craken.node.crud.Filters;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.ListUtil;

import org.apache.commons.lang.reflect.MethodUtils;
import org.apache.lucene.search.Filter;

public final class BinaryExpression extends ValueObject implements Expression {
	public final Expression left;
	public final Expression right;
	public final Op operator;

	public BinaryExpression(Expression left, Op op, Expression right) {
		this.left = left;
		this.operator = op;
		this.right = right;
	}

	public Comparable value(NodeCommonMap node) {
		if (operator == Op.EQ && left instanceof QualifiedNameExpression && "exist".equals(((QualifiedNameExpression)left).lastName())) {
			return right.value(node) != null;
		}
		return operator.compute(left.value(node), right.value(node)) ;	
	}
	
	public Filter filter(){
		try {
			if (Op.AND == operator || Op.OR == operator){
				Filter f1 = (Filter) MethodUtils.invokeMethod(left, "filter", new Object[0]) ;
				Filter f2 = (Filter) MethodUtils.invokeMethod(right, "filter", new Object[0]) ;
				if (operator == Op.AND) return Filters.and(f1, f2) ;
				if (operator == Op.OR) return Filters.or(f1, f2) ;
			} else if (Op.EQ == operator || Op.LT == operator || Op.LE == operator || Op.GT == operator || Op.GE == operator || Op.CONTAIN == operator){
				
				if (Op.EQ == operator && left instanceof QualifiedNameExpression && right instanceof QualifiedNameExpression){
					QualifiedNameExpression fld = findExistField() ;
					return Filters.exists(fld.lastName()) ;
				}
				
				QualifiedNameExpression qne = findQName() ;
				ConstantExpression con = findConstant();
				return con.filter(operator, qne) ;
			} else if (Op.IN == operator){
				QualifiedNameExpression qne = (QualifiedNameExpression) left ;
				TupleExpression te = (TupleExpression) right ;
				List<String> values = ListUtil.newList() ;
				for (Expression expr : te.expressions()){
					values.add(((ConstantExpression)expr).constantValue().toString()) ;
				}
				return Filters.in(qne.lastName(), values.toArray(new String[0])) ;
			} else if (Op.NE == operator) {
				 return Filters.ne(findQName().qname.last(), findConstant().constantValue().toString()) ;
			}
			
		} catch(ClassCastException e){
			throw new UnsupportedOperationException(e) ;
		} catch (NoSuchMethodException e) {
			throw new UnsupportedOperationException(e) ;
		} catch (IllegalAccessException e) {
			throw new UnsupportedOperationException(e) ;
		} catch (InvocationTargetException e) {
			throw new UnsupportedOperationException(e) ;
		}
		
		throw new IllegalArgumentException() ;
	}
	
	private QualifiedNameExpression findExistField() {
		if  ( "exist".equals( ((QualifiedNameExpression)left).lastName())) return (QualifiedNameExpression) right ;
		return (QualifiedNameExpression) left ;
	}

	private QualifiedNameExpression findQName(){
		if (left instanceof QualifiedNameExpression) return (QualifiedNameExpression) left ;
		if (right instanceof QualifiedNameExpression) return (QualifiedNameExpression) right ;
		throw new IllegalArgumentException("illegal where expression : not found NameExpression") ;
	}

	private ConstantExpression findConstant(){
		if (left instanceof ConstantExpression) return (ConstantExpression) left ;
		if (right instanceof ConstantExpression) return (ConstantExpression) right ;
		if (right instanceof QualifiedNameExpression && ArrayUtil.contains(new String[]{"true", "false"}, ((QualifiedNameExpression)right).lastName())){
			String value = ((QualifiedNameExpression)right).lastName() ;
			return new BooleanExpression(Boolean.valueOf(value)) ;
		}
		if (left instanceof QualifiedNameExpression && ArrayUtil.contains(new String[]{"true", "false"}, ((QualifiedNameExpression)left).lastName())){
			String value = ((QualifiedNameExpression)left).lastName() ;
			return new BooleanExpression(Boolean.valueOf(value)) ;
		}
		throw new IllegalArgumentException("illegal where expression : not found ConstantExpression") ;
	}

}