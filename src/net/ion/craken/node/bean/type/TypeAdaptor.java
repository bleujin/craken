package net.ion.craken.node.bean.type;

import java.lang.reflect.Field;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.bean.TypeStrategy;
import net.ion.craken.tree.PropertyValue;

public abstract class TypeAdaptor<T> {

	public abstract T read(TypeStrategy ts, Field field, NodeCommon node)  ;

}



