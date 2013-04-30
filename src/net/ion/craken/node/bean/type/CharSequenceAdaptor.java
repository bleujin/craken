package net.ion.craken.node.bean.type;

import java.lang.reflect.Field;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.bean.TypeStrategy;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.ObjectUtil;

public class CharSequenceAdaptor extends TypeAdaptor<CharSequence> {

	@Override
	public CharSequence read(TypeStrategy ts, Field field, NodeCommon node) {
		final Object value = node.property(field.getName()).value();
		return CharSequence.class.isAssignableFrom(value.getClass()) ? (CharSequence)value : ObjectUtil.toString(value);
	}

}
