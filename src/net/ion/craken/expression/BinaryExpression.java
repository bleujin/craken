package net.ion.craken.expression;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.crud.Filters;
import net.ion.framework.util.Debug;

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

	public Comparable value(NodeCommon node) {
		return operator.compute(left.value(node), right.value(node)) ;	
	}
	
	public Filter filter(){
		try {
			if (Op.AND == operator || Op.OR == operator){
				BinaryExpression eleft = (BinaryExpression) left;
				BinaryExpression rleft = (BinaryExpression) right;
				if (operator == Op.AND) return Filters.and(eleft.filter(), rleft.filter()) ;
				if (operator == Op.OR) return Filters.or(eleft.filter(), rleft.filter()) ;
			} else if (Op.EQ == operator || Op.LT == operator || Op.LE == operator || Op.GT == operator || Op.GE == operator || Op.CONTAIN == operator){
				QualifiedNameExpression qne = findQName() ;
				ConstantExpression con = findConstant();
				return con.filter(operator, qne) ;
			} else if (Op.IN == operator){
				Debug.line();
			}
		} catch(ClassCastException e){
			throw new UnsupportedOperationException(e) ;
		}
		
		Debug.line(left, right, operator) ;
		return null ;
	}
	
	private QualifiedNameExpression findQName(){
		if (left instanceof QualifiedNameExpression) return (QualifiedNameExpression) left ;
		if (right instanceof QualifiedNameExpression) return (QualifiedNameExpression) right ;
		throw new IllegalArgumentException("illegal where expression : not found NameExpression") ;
	}

	private ConstantExpression findConstant(){
		if (left instanceof ConstantExpression) return (ConstantExpression) left ;
		if (right instanceof ConstantExpression) return (ConstantExpression) right ;
		throw new IllegalArgumentException("illegal where expression : not found ConstantExpression") ;
	}

}