package net.ion.craken.node.bean;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.bean.type.TypeAdaptor;
import net.ion.craken.node.crud.ReadNodeImpl;
import net.ion.craken.tree.PropertyId;
import net.ion.framework.util.ArrayUtil;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

public interface TypeStrategy {
	public final static TypeStrategy DEFAULT = new DefaultTypeStrategy() ;
	
	public boolean supported(Field field, ReadNode node) ;
	public Boolean resolveAdaptor(Object target, Field field, ReadNode node) throws IllegalArgumentException, IllegalAccessException ;
	
}

class DefaultTypeStrategy implements TypeStrategy {

	private static Map<Class<?>, Class<?>> PRIMITIVE_TYPE ;
	private PrimitiveAdaptor primitivePropertyAdaptor = new PrimitiveAdaptor();
	private ReferenceAdaptor referenceAdpator = new ReferenceAdaptor();
	private ReferencesAdaptor referencesAdpator = new ReferencesAdaptor();
	private ValueObjectAdaptor valueObjectPropertyAdaptor = new ValueObjectAdaptor() ;
	private ChildBeanAdaptor childBeanAdaptor = new ChildBeanAdaptor();
	private PrimitiveArrayAdaptor primitiveArrayPropertyAdaptor = new PrimitiveArrayAdaptor();
	private CollectionValueObject collectionPropertyAdaptor = new CollectionValueObject() ;


	public boolean supported(Field field, ReadNode node){
		return node.hasProperty(PropertyId.normal(field.getName())) || node.hasChild(field.getName()) || node.hasProperty(PropertyId.refer(field.getName()));
	}

	@Override
	public Boolean resolveAdaptor(Object target, Field field, ReadNode node) throws IllegalArgumentException, IllegalAccessException {
		final TypeAdaptor adaptor = findAdaptor(field, node);
		if (adaptor == null) return Boolean.FALSE ;
		field.set(target, adaptor.read(this, field, node)) ;

		return Boolean.TRUE ;
	}
	
	private TypeAdaptor findAdaptor(Field field, ReadNode node) {
		
		if (node.hasProperty(PropertyId.normal(field.getName()))){
			if (PRIMITIVE_TYPE.containsKey(field.getType())) {
				return primitivePropertyAdaptor;
			} else if (PRIMITIVE_TYPE.containsValue(field.getType())) {
				return primitivePropertyAdaptor ;
			} else if (field.getType().isArray() && ( PRIMITIVE_TYPE.containsKey(field.getType().getComponentType()) || PRIMITIVE_TYPE.containsValue(field.getType().getComponentType())) ) {
				return primitiveArrayPropertyAdaptor ;
			} else if (Set.class.isAssignableFrom(field.getType())) {
				return collectionPropertyAdaptor ;
			} else {
				return valueObjectPropertyAdaptor ;
			}
		} else if (node.hasChild(field.getName())) {
			return childBeanAdaptor ;
		} else if (node.refs(field.getName()).hasNext()) {
			if (List.class.isAssignableFrom(field.getType())){
				return referencesAdpator ;
			} else {
				return referenceAdpator ;
			}
		}

		return null;
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
		PRIMITIVE_TYPE = Collections.unmodifiableMap(primToWrap);
	}

}

class PrimitiveArrayAdaptor extends TypeAdaptor<Object> {

	@Override
	public Object read(TypeStrategy ts, Field field, ReadNode node) {
		
		final Set values = node.property(field.getName()).asSet();
		if (Byte.class.isAssignableFrom(field.getType().getComponentType())) {
			return (Byte[])values.toArray(new Byte[0]) ;
		} else if (Boolean.class.isAssignableFrom(field.getType().getComponentType())) {
			return (Boolean[])values.toArray(new Boolean[0]) ;
		} else if (Character.class.isAssignableFrom(field.getType().getComponentType())) {
			return (Character[])values.toArray(new Character[0]) ;
		} else if (Double.class.isAssignableFrom(field.getType().getComponentType())) {
			return (Double[])values.toArray(new Double[0]) ;
		} else if (Float.class.isAssignableFrom(field.getType().getComponentType())) {
			return (Float[])values.toArray(new Float[0]) ;
		} else if (Integer.class.isAssignableFrom(field.getType().getComponentType())) {
			return (Integer[])values.toArray(new Integer[0]) ;
		} else if (Long.class.isAssignableFrom(field.getType().getComponentType())) {
			return (Long[])values.toArray(new Long[0]) ;
		} else if (Short.class.isAssignableFrom(field.getType().getComponentType())) {
			return (Short[])values.toArray(new Short[0]) ;
			
			
		} else if (byte.class.isAssignableFrom(field.getType().getComponentType())) {
			return ArrayUtil.toPrimitive((Byte[])values.toArray(new Byte[0])) ;
		} else if (boolean.class.isAssignableFrom(field.getType().getComponentType())) {
			return ArrayUtil.toPrimitive((Boolean[])values.toArray(new Boolean[0])) ;
		} else if (char.class.isAssignableFrom(field.getType().getComponentType())) {
			return ArrayUtil.toPrimitive((Character[])values.toArray(new Character[0])) ;
		} else if (double.class.isAssignableFrom(field.getType().getComponentType())) {
			return ArrayUtil.toPrimitive((Double[])values.toArray(new Double[0])) ;
		} else if (float.class.isAssignableFrom(field.getType().getComponentType())) {
			return ArrayUtil.toPrimitive((Float[])values.toArray(new Float[0])) ;
		} else if (int.class.isAssignableFrom(field.getType().getComponentType())) {
			return ArrayUtil.toPrimitive((Integer[])values.toArray(new Integer[0])) ;
		} else if (long.class.isAssignableFrom(field.getType().getComponentType())) {
			return ArrayUtil.toPrimitive((Long[])values.toArray(new Long[0])) ;
		} else if (short.class.isAssignableFrom(field.getType().getComponentType())) {
			return ArrayUtil.toPrimitive((Short[])values.toArray(new Short[0])) ;
		}
		
		return values.toArray() ;
	}

}

class ValueObjectAdaptor extends TypeAdaptor<Object> {

	@Override
	public Object read(TypeStrategy ts, Field field, ReadNode node) {
		return node.property(field.getName()).value();
	}

}


class ChildBeanAdaptor extends TypeAdaptor<Object>{

	@Override
	public Object read(TypeStrategy ts, Field field, ReadNode  node) {
		ReadNode child = node.child(field.getName()) ;
		return field.getType().cast(ProxyBean.create(ts, child, (Class)field.getType())) ;
	}

}


class PrimitiveAdaptor extends TypeAdaptor {

	@Override
	public Object read(TypeStrategy ts, Field field, ReadNode node) {
		return node.property(field.getName()).value();
	}

}

class CollectionValueObject extends TypeAdaptor<Object> {

	@Override
	public Object read(TypeStrategy ts, Field field, ReadNode node) {
		Set values = node.property(field.getName()).asSet();
		return values ;
	}

	
}

class ReferencesAdaptor extends TypeAdaptor<Object>{

	@Override
	public Object read(TypeStrategy ts, Field field, ReadNode node) {
		IteratorList<ReadNode> refs = node.refs(field.getName());
		List list = ListUtil.newList() ;
		
		Type genericFieldType = field.getGenericType();
		Class findGenericClz = null;
		if (genericFieldType instanceof ParameterizedType){
			 ParameterizedType aType = (ParameterizedType) genericFieldType;
			 findGenericClz = (Class)aType.getActualTypeArguments()[0];
		}
		
		while(refs.hasNext()){
			final ReadNode next = refs.next();
			list.add(findGenericClz.cast(ProxyBean.create(ts, next, findGenericClz))) ;
		}
		
		return list;
	}

}

class ReferenceAdaptor extends TypeAdaptor<Object>{

	@Override
	public Object read(TypeStrategy ts, Field field, ReadNode node) {
		ReadNode refbean = node.ref(field.getName());
		return field.getType().cast(ProxyBean.create(ts, refbean, (Class)field.getType())) ;
	}

}
