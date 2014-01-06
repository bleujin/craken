package net.ion.craken.tree;

import net.ion.craken.node.NodeCommon;
import net.ion.framework.util.StringUtil;

public class ExtendPropertyId {

	private String path ;
	public ExtendPropertyId(String propPath) {
		if (propPath == null) throw new IllegalArgumentException("propPath should be not null") ;
		this.path = propPath ;
	}

	public static ExtendPropertyId create(String propPath) {
		return new ExtendPropertyId(propPath);
	}

	public PropertyValue propValue(NodeCommon node) {
		return propValue(path, node) ;
	}

	
	private PropertyValue propValue(String propPath, NodeCommon node){
		if (! StringUtil.containsAny(propPath, new char[]{'/', '@'})){
			return node.property(propPath) ;
		} 
		
		StringBuilder prefix = new StringBuilder() ;
		for(char c : propPath.toCharArray()){
			if (c == '/'){
				if ("".equals(prefix.toString())){ // absolute path
					String firstPath = StringUtil.substringBetween(propPath, "/", "/") ;
					return propValue(StringUtil.substringAfter(propPath, firstPath + "/"), node.root().child(firstPath)) ;
				} else if ("..".equals(prefix.toString())) {
					return propValue(StringUtil.substringAfter(propPath, prefix.toString() + "/"), node.parent()) ;
				} else if (node.hasChild(prefix.toString())){
					return propValue(StringUtil.substringAfter(propPath, prefix.toString() + "/"), node.child(prefix.toString())) ;
				}
			} else if (c == '@' && node.hasRef(prefix.toString())) {
				return propValue(StringUtil.substringAfter(propPath, prefix.toString() + "@"), node.ref(prefix.toString())) ;
			}
			prefix.append(c) ;
		}

		return node.property(propPath);
	}
	
}
