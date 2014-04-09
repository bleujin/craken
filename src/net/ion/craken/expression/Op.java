package net.ion.craken.expression;

import java.math.BigDecimal;

import net.ion.framework.util.Debug;
import net.ion.framework.util.ObjectUtil;
import net.ion.framework.util.StringUtil;


public enum Op {
	PLUS{
		public Comparable compute(Comparable left, Comparable right) {
			if (left instanceof BigDecimal && right instanceof BigDecimal) {
				return ((BigDecimal)left).add((BigDecimal) right) ;
			} else {
				return ObjectUtil.toString(left) + ObjectUtil.toString(right);
			}
		}		
	}, MINUS{
		public Comparable compute(Comparable left, Comparable right) {
			if (left instanceof BigDecimal && right instanceof BigDecimal) {
				return ((BigDecimal)left).subtract( (BigDecimal) right) ;
			} else {
				throw new ArithmeticException() ;
			}
		}		
	}, MUL{
		public Comparable compute(Comparable left, Comparable right) {
			if (left instanceof BigDecimal && right instanceof BigDecimal) {
				return ((BigDecimal)left).multiply((BigDecimal) right) ;
			} else {
				throw new ArithmeticException() ;
			}
		}		
	}, DIV{
		public Comparable compute(Comparable left, Comparable right) {
			if (left instanceof BigDecimal && right instanceof BigDecimal) {
				return ((BigDecimal)left).divide( (BigDecimal) right) ;
			} else {
				throw new ArithmeticException() ;
			}
		}		
	}, MOD{
		public Comparable compute(Comparable left, Comparable right) {
			if (left instanceof BigDecimal && right instanceof BigDecimal) {
				return ((BigDecimal)left).remainder((BigDecimal) right) ;
			} else {
				throw new ArithmeticException() ;
			}
		}		
	}, NEG {
		public Comparable compute(Comparable operand) {
			if (operand instanceof BigDecimal) return ((BigDecimal)operand).negate() ;
			throw new ArithmeticException() ;
		}
	}, 
	
	
	
	// BinaryExpression
	IN {
		@Override
		public Boolean compute(Comparable left, Comparable right) {
			if (left == null || right == null || (! (right instanceof ComparableSet))) return false ;
			return ((ComparableSet)right).contains(left) ;
		}
		
	}, NOT_IN{
		@Override
		public Boolean compute(Comparable left, Comparable right) {
			if (left == null || right == null || (! (right instanceof ComparableSet))) return false ;
			return ! ((ComparableSet)right).contains(left) ;
		}
	}, CONTAIN{
		@Override
		public Boolean compute(Comparable left, Comparable right) {
			return isNotNull(left, right) && StringUtil.indexOf(left.toString(), right.toString()) > -1 ;
		}
	}, EQ{
		@Override
		public Boolean compute(Comparable left, Comparable right) {
			
			
			return isNotNull(left, right) && left.equals(right) ;
		}
	}, GT {
		@Override
		public Boolean compute(Comparable left, Comparable right) {
			return isNotNull(left, right) && isSameComparable(left, right) && left.compareTo(right) > 0;
		}
	}, LT {
		@Override
		public Boolean compute(Comparable left, Comparable right) {
			return isNotNull(left, right) && isSameComparable(left, right) && left.compareTo(right) < 0;
		}
	}, GE{
		@Override
		public Boolean compute(Comparable left, Comparable right) {
			return isNotNull(left, right) && isSameComparable(left, right) && left.compareTo(right) >= 0;
		}
	}, LE{
		@Override
		public Boolean compute(Comparable left, Comparable right) {
			return isNotNull(left, right) && isSameComparable(left, right) && left.compareTo(right) <= 0;
		}
		
		
	}, NE {
		@Override
		public Boolean compute(Comparable left, Comparable right) {
			return !(isNotNull(left, right) && left.equals(right));
		}
	}, 
	
	
	
	// logical
	
	NOT {
		public Boolean compute(Comparable operand) {
			return Boolean.FALSE.equals(operand);
		}
	}, AND{
		public Boolean compute(Comparable left, Comparable right) {
			return Boolean.TRUE.equals(left) && Boolean.TRUE.equals(right) ;
		}
	}, OR{
		public Boolean compute(Comparable left, Comparable right) {
			return Boolean.TRUE.equals(left) || Boolean.TRUE.equals(right) ;
		}
	}, IS{
		public Boolean compute(Comparable left, Comparable right) {
			return left == null;
		}
	} ;


	protected boolean isNotNull(Comparable left, Comparable right){
		return left != null && right != null ; 
	}

	protected boolean isSameComparable(Comparable left, Comparable right){
		return left.getClass().equals(right.getClass()) ; 
	}

	
	public boolean isContains(Op... ops) {
		for(Op op : ops){
			if (op == this) return true ;
		}
		return false;
	}
	
	public Comparable compute(Comparable left, Comparable right) {
		return false;
	}
	
	public Comparable compute(Comparable value) {
		return false;
	}

}
