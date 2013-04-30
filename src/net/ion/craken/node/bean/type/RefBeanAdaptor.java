package net.ion.craken.node.bean.type;

import java.lang.reflect.Field;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.bean.ProxyBean;
import net.ion.craken.node.bean.TypeStrategy;
import net.ion.craken.tree.PropertyValue;

public class RefBeanAdaptor extends TypeAdaptor<Object>{

	@Override
	public Object read(TypeStrategy ts, Field field, NodeCommon  node) {
		return new ProxyBean(ts, node) ;
	}

}
