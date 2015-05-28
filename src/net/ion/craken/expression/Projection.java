package net.ion.craken.expression;

import net.ion.craken.node.ReadNode;
import net.ion.framework.util.StringUtil;

public final class Projection extends ValueObject {
	private final Expression expression;
	private final String alias;

	public Projection(Expression expression, String alias) {
		this.expression = expression;
		this.alias = alias;
	}
	
	public Object value(ReadNode node){
		return expression.value(node) ;
	}

	
	public String label(){
		if (StringUtil.isNotBlank(alias)){
			return alias ;
		}
		if (expression instanceof QualifiedNameExpression){
			return ((QualifiedNameExpression)expression).lastName() ;
		}
		
		return "";
	}
}
