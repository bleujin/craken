package net.ion.craken.node.bean.type;

import java.lang.reflect.Field;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.bean.ProxyBean;
import net.ion.craken.node.bean.TypeStrategy;
import net.ion.craken.tree.PropertyValue;

public class ChildBeanAdaptor extends TypeAdaptor<Object>{

	@Override
	public Object read(TypeStrategy ts, Field field, NodeCommon  node) {
		NodeCommon child = node.child(field.getName()) ;
		return field.getType().cast(ProxyBean.create(ts, child, (Class)field.getType())) ;
	}

}
