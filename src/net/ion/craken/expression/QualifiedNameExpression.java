package net.ion.craken.expression ;

import java.util.Iterator;
import java.util.Set;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.NumberUtil;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;


public final class QualifiedNameExpression extends ValueObject implements Expression {
	public final QualifiedName qname;

	public QualifiedNameExpression(QualifiedName qname) {
		this.qname = qname;
	}

	public static QualifiedNameExpression of(String... names) {
		return new QualifiedNameExpression(QualifiedName.of(names));
	}

	public String lastName(){
		return qname.names.get(qname.names.size() -1) ;
	}
	
	
	@Override
	public Comparable value(NodeCommon node) {
		final Iterator<String> iter = qname.names.iterator();
		
		NodeCommon findNode = node ;
		String nextString = null ;
		while(iter.hasNext()){
			nextString = StringUtil.trim(iter.next()) ;
			
			if ("true".equalsIgnoreCase(nextString) && ! iter.hasNext()) return Boolean.TRUE ;
			if ("false".equalsIgnoreCase(nextString) && ! iter.hasNext()) return Boolean.FALSE ;
			
			
			if ("this".equalsIgnoreCase(nextString) ) continue ;
			if ("parent".equalsIgnoreCase(nextString) ){
				findNode = findNode.parent() ;
				continue ;
			}
			if (iter.hasNext() && findNode.hasChild(nextString)){
				findNode = findNode.child(nextString) ;
			} else if (iter.hasNext() && findNode.hasRef(nextString)) {
				findNode = findNode.ref(nextString) ;
			} else if (iter.hasNext()){
				return null ;
			}
		}
		
		if (nextString.startsWith("[") && nextString.endsWith("]")){
			String propId = StringUtil.substringBetween(nextString, "[", "]") ;
			return new SetComparable(findNode.property(propId).asSet()) ;
		}
		
		
		Object value = findNode.property(nextString).value() ;
		if (value != null && (value instanceof Integer || value instanceof Long || value instanceof Float || value instanceof Double)){
			return NumberUtil.createBigDecimal(value.toString()) ;
		}
		return (Comparable)value ;
	}
	
}



