package net.ion.craken.tree;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.StringUtil;

public class ExtendPropertyId {

	private String propPath ;
	public ExtendPropertyId(String propPath) {
		if (propPath == null) throw new IllegalArgumentException("propPath should be not null") ;
		this.propPath = propPath ;
	}

	public static ExtendPropertyId create(String propPath) {
		return new ExtendPropertyId(propPath);
	}

	public PropertyValue propValue(NodeCommon node) {
		if (! StringUtil.containsAny(propPath, new char[]{'/', '@'})){
			return node.property(propPath) ;
		} 
		
		StringBuilder prefix = new StringBuilder() ;
		for(char c : propPath.toCharArray()){
			if (c == '/'){
				if ("..".equals(prefix.toString())) {
					return new ExtendPropertyId(StringUtil.substringAfter(propPath, prefix.toString() + "/")).propValue(node.parent()) ;
				} else if (node.hasChild(prefix.toString())){
					return new ExtendPropertyId(StringUtil.substringAfter(propPath, prefix.toString() + "/")).propValue(node.child(prefix.toString())) ;
				}
			} else if (c == '@' && node.hasRef(prefix.toString())) {
				return new ExtendPropertyId(StringUtil.substringAfter(propPath, prefix.toString() + "@")).propValue(node.ref(prefix.toString())) ;
			}
			prefix.append(c) ;
		}

		return node.property(propPath);
	}

	
}
