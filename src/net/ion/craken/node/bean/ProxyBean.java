package net.ion.craken.node.bean;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

public class ProxyBean implements MethodInterceptor{

	
	private NodeCommon node ;
	private volatile boolean initialized = false ;
	private TypeStrategy ts ;
	public ProxyBean(TypeStrategy ts, NodeCommon node) {
		this.ts = ts ;
		this.node = node ;
	}

	public final static <T> T create(TypeStrategy ts, NodeCommon node2, Class<T> clz){
		try {
			
			Enhancer e = new Enhancer();
			e.setSuperclass(clz);
			e.setCallback(new ProxyBean(ts, node2));
			
			
			return (T)e.create();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Error(e.getMessage());
		}

	}
	
	public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy) throws Throwable {
		synchronized (this) {
			while (! initialized) {
				for(Field field : obj.getClass().getSuperclass().getDeclaredFields()){
					if (Modifier.isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) continue ;
					
					if (ts.supported(field, node)){
						field.setAccessible(true) ;
						field.set(obj, ts.resolveAdaptor(field, node)) ;
					}
				}
				initialized = true ;
			}
		}
		
		
		return proxy.invokeSuper(obj, args);
	}
	
	private static Class[] HandleType = new Class[]{String.class, } ;
}

