package net.ion.script.rhino;

import java.io.StringReader;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import net.ion.framework.util.MapUtil;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class TestFirst extends TestCase {

	public void testFunction() throws Exception {
		String script = "function abc(x,y) {return x+y;}";
		Context context = Context.enter();
		try {
			ScriptableObject scope = context.initStandardObjects();
			Scriptable that = context.newObject(scope);
			Function fct = context.compileFunction(scope, script, "script", 1, null);
			Object result = fct.call(context, scope, that, new Object[] { 2, 3 });
			System.out.println(Context.jsToJava(result, int.class));
		} finally {
			Context.exit();
		}
	}

	public void testFindFunction() throws Exception {
		String script = "function abc(x,y) {return x+y;}" + "function def(u,v) {return u-v;}";
		Context context = Context.enter();
		try {
			ScriptableObject scope = context.initStandardObjects();
			context.evaluateString(scope, script, "script", 1, null);
			
			Function fct = (Function) scope.get("abc", scope);
			Object result = fct.call(context, scope, scope, new Object[] { 2, 3 });
			System.out.println(Context.jsToJava(result, int.class));
		} finally {
			Context.exit();
		}
	}
	
	public void testInFunction() throws Exception {
		String script = "new function(){"
				+ " this.hello = function(name) {return 'Hello ' + name; }, "
				+ " this.hi = function(name) {return 'Hi ' + name; } "
				+ "} ;" ;
		
		Context context = Context.enter();
		try {
			ScriptableObject scope = context.initStandardObjects();
			
			
			Object object = context.evaluateString(scope, script, "script", 1, null);
			
			
			Object prop = ((Scriptable) object).get("hi", null) ;
			Object result = ((Function)prop).call(context, (Scriptable)object, scope, new Object[]{"bleujin"})  ;
			System.out.println(Context.jsToJava(result, String.class));
		} finally {
			Context.exit();
		}
	}

	public void testRhinerSimple() throws Exception {
		final Rhiner rhiner = Rhiner.create().start() ;
		String script = "new function(){"
				+ " this.hello = function(name) {return 'Hello ' + name; }, "
				+ " this.hi = function(greeting, name) {return greeting + ' ' + name; } "
				+ "} ;" ;
		
		rhiner.define("script", new StringReader(script)) ;
		String actual = rhiner.callFn("script.hi", String.class, "hi", "bleujin") ;
		assertEquals("hi bleujin", actual);
	}
	
	public void testBinding() throws Exception {
		final Rhiner rhiner = Rhiner.create().start() ;
		rhiner.bind("map", MapUtil.create("name", "hero")) ;

		String script = "new function(){"
				+ " this.hello = function(name) {return 'Hello ' + name; }, "
				+ " this.hi = function(greeting, name) {"
				+ "	print(greeting) ;"
				+ "	return greeting + ' ' + name + ' ' + map.get('name'); } "
				+ "} ;" ;
		
		rhiner.define("script", new StringReader(script)) ;
		String actual = rhiner.callFn("script.hi", String.class, "hi", "bleujin") ;
		assertEquals("hi bleujin hero", actual);
		rhiner.shutdown();
	}
	
	
	public void testInOtherThread() throws Exception {
		final Rhiner rhiner = Rhiner.create().start() ;
		String script = "new function(){"
				+ " this.hello = function(name) {return 'Hello ' + name; }, "
				+ " this.hi = function(greeting, name) {return greeting + ' ' + name; } "
				+ "} ;" ;
		
		rhiner.define("script", new StringReader(script)) ;

		new Thread(new Runnable(){
			@Override
			public void run() {
				String result = rhiner.callFn("script.hi", String.class, "hi", "bleujin") ;
				Debug.line(result);
			}
			
		}).start();
		
		
		Thread.sleep(1000);
		
		rhiner.shutdown() ;
	}
	
	

}
