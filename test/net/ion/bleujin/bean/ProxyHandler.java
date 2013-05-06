package net.ion.bleujin.bean;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import net.ion.craken.node.ReadNode;
import net.ion.framework.util.Debug;

public class ProxyHandler implements InvocationHandler{

	private Class<? extends ProxyIf> clz ;
	private ReadNode node ;

	public ProxyHandler(Class<? extends ProxyIf> clz, ReadNode node) {
		this.clz = clz ;
		this.node = node ;
	}

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		
		Constructor<? extends ProxyIf> cons = clz.getDeclaredConstructor();
		ProxyIf instance = cons.newInstance();
		
		for (Field field : clz.getDeclaredFields()) {
			Debug.line(field.getName(), field.getType()) ;
		}

		return method.invoke(instance, args);
	}

}
