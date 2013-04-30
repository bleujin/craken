package net.ion.craken.node.bean;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import javax.xml.crypto.Data;

import org.apache.ecs.xhtml.pre;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.bean.type.CharSequenceAdaptor;
import net.ion.craken.node.bean.type.ChildBeanAdaptor;
import net.ion.craken.node.bean.type.PrimitiveAdaptor;
import net.ion.craken.node.bean.type.RefBeanAdaptor;
import net.ion.craken.node.bean.type.TypeAdaptor;
import net.ion.craken.tree.PropertyId;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public interface TypeStrategy {
	public final static TypeStrategy DEFAULT = new DefaultTypeStrategy() ;
	
	public boolean supported(Field field, NodeCommon node) ;
	public Object resolveAdaptor(Field field, NodeCommon node) ;
	
}

class DefaultTypeStrategy implements TypeStrategy {

	private static Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER_TYPE ;
	private PrimitiveAdaptor primitive = new PrimitiveAdaptor();
	private TypeAdaptor anony = new RefBeanAdaptor();
	private TypeAdaptor charSeqType = new CharSequenceAdaptor() ;
	private TypeAdaptor childBean = new ChildBeanAdaptor();


	public boolean supported(Field field, NodeCommon node){
		return node.hasProperty(PropertyId.normal(field.getName())) || node.hasChild(field.getName()) || node.hasProperty(PropertyId.refer(field.getName()));
	}

	@Override
	public Object resolveAdaptor(Field field, NodeCommon node) {
		return findAdaptor(field, node).read(this, field, node) ;
	}
	
	private TypeAdaptor findAdaptor(Field field, NodeCommon node) {
		
		if (node.hasProperty(PropertyId.normal(field.getName()))){
			if (PRIMITIVE_TO_WRAPPER_TYPE.containsKey(field.getType())) {
				return primitive;
			} else if (PRIMITIVE_TO_WRAPPER_TYPE.containsValue(field.getType())) {
				return primitive ;
			} else if (CharSequence.class.isAssignableFrom(field.getType())) {
				return charSeqType ;
			} else if (Date.class.isAssignableFrom(field.getType())) {
				return primitive ;
			}

			return anony;
		} else if (node.hasChild(field.getName())) {
			return childBean ;
		} else if (node.refs(field.getName()).hasNext()) {
			
		}

		return anony;
	}

	static {
		BiMap<Class<?>, Class<?>> primToWrap = HashBiMap.create();
		primToWrap.put(boolean.class, Boolean.class);
		primToWrap.put(byte.class, Byte.class);
		primToWrap.put(char.class, Character.class);
		primToWrap.put(double.class, Double.class);
		primToWrap.put(float.class, Float.class);
		primToWrap.put(int.class, Integer.class);
		primToWrap.put(long.class, Long.class);
		primToWrap.put(short.class, Short.class);
		primToWrap.put(void.class, Void.class);
		PRIMITIVE_TO_WRAPPER_TYPE = Collections.unmodifiableMap(primToWrap);
	}

}
