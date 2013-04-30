package net.ion.craken.node.bean;

import net.sf.cglib.proxy.*;
import java.util.*;

/***
 * 
 * @author baliuka
 */
public class Trace implements MethodInterceptor {

	private int ident = 1;

	/*** Creates a new instance of Trace */
	private Trace() {
	}

	public static Object newInstance(Class clazz) {
		try {
			Enhancer e = new Enhancer();
			e.setSuperclass(clazz);
			e.setCallback(new Trace());
			return e.create();
		} catch (Throwable e) {
			e.printStackTrace();
			throw new Error(e.getMessage());
		}

	}

	public Object intercept(Object obj, java.lang.reflect.Method method, Object[] args, MethodProxy proxy) throws Throwable {
		printIdent(ident);
		println(method);
		for (int i = 0; i < args.length; i++) {
			printIdent(ident);
			print("arg" + (i + 1) + ": ");
			if (obj == args[i])
				println("this");
			else
				println(args[i]);
		}
		ident++;

		Object retValFromSuper = null;
		try {
			retValFromSuper = proxy.invokeSuper(obj, args);
			ident--;
		} catch (Throwable t) {
			ident--;
			printIdent(ident);
			println("throw " + t);
			println();
			throw t.fillInStackTrace();
		}

		printIdent(ident);
		System.out.print("return ");
		if (obj == retValFromSuper)
			println("this");
		else
			println(retValFromSuper);

		if (ident == 1)
			println();

		return retValFromSuper;
	}

	void printIdent(int ident) {

		while (--ident > 0) {
			print(".......");
		}
		print("  ");
	}
	
	private void println(Object... msg){
		System.out.println(Arrays.toString(msg)) ;
	}
	private void print(String msg){
		System.out.print(msg) ;
	}

}
