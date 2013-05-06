package net.ion.craken.node.convert.bean.type;

import java.lang.reflect.Field;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.bean.TypeStrategy;

public abstract class TypeAdaptor<T> {

	public abstract T read(TypeStrategy ts, Field field, ReadNode node)  ;

}



