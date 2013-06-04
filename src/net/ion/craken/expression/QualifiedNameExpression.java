package net.ion.craken.expression ;

import java.util.Iterator;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.NumberUtil;


public final class QualifiedNameExpression extends ValueObject implements Expression {
	public final QualifiedName qname;

	public QualifiedNameExpression(QualifiedName qname) {
		this.qname = qname;
	}

	public static QualifiedNameExpression of(String... names) {
		return new QualifiedNameExpression(QualifiedName.of(names));
	}

	
	
	
	@Override
	public Comparable value(NodeCommon node) {
		final Iterator<String> iter = qname.names.iterator();
		
		NodeCommon findNode = node ;
		String nextString = null ;
		while(iter.hasNext()){
			nextString = iter.next() ;
			
			if ("true".equalsIgnoreCase(nextString) && ! iter.hasNext()) return Boolean.TRUE ;
			if ("false".equalsIgnoreCase(nextString) && ! iter.hasNext()) return Boolean.FALSE ;
			
			
			if ("this".equalsIgnoreCase(nextString)) continue ;
			if (findNode.hasChild(nextString)){
				findNode = findNode.child(nextString) ;
			} else if (findNode.hasRef(nextString)) {
				findNode = findNode.ref(nextString) ;
			} else if (iter.hasNext()){
				return null ;
			}
		}
		
		final Object value = findNode.property(nextString).value();
		if (value != null && (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double)){
			return NumberUtil.createBigDecimal(value.toString()) ;
		}
		return (Comparable) value;
	}
}
