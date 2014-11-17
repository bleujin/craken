package net.ion.script.rhino;

import java.io.StringWriter;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

import junit.framework.TestCase;
import net.ion.framework.util.Debug;
import sun.org.mozilla.javascript.internal.NativeJavaObject;

public class TestFirst extends TestCase {

	public void testFunction() throws Exception {
		ScriptEngineManager manager = new ScriptEngineManager();
		ScriptEngine se = manager.getEngineByName("JavaScript");
		
		String script = "new function(){"
				+ " 	this.sum = function(x,y) {return x+y;}"
				+ "};";
		Object fn = se.eval(script) ;
		
		Object result = ((Invocable)se).invokeMethod(fn, "sum", 1, 2) ;
		if (result instanceof NativeJavaObject){
			result = ((NativeJavaObject)result).unwrap() ;
		}
		Debug.line(result);

	}

	
	public void testCallFunction() throws Exception {
		Scripter r = Scripter.create() ;
		String script = "new function(){"
				+ " 	this.sum = function(x,y) {return x+y;}"
				+ "};";
		r.define("sample", script) ;
		
		
		Object result = r.callFn("sample.sum", RhinoResponse.ReturnNative, 1, 2) ;
		assertEquals(Double.class, result.getClass());
		assertEquals(3D, Double.parseDouble(result.toString()) );
	}
	
	public void testSysout() throws Exception {

		Scripter r = Scripter.create() ;
		String script = "new function(){"
				+ " 	this.hello = function() { java.lang.System.out.println('Hello')}"
				+ "};";
		r.define("sample", script) ;
		
		
		Object result = r.callFn("sample.hello", RhinoResponse.ReturnNative) ;
	}
	
	
	public void testBinding() throws Exception {
		Scripter r = Scripter.create() ;
		String script = "new function(){"
				+ " 	this.hello = function() {writer.write('Hello') ; return writer }"
				+ "};";
		r.define("sample", script) ;
		StringWriter writer = new StringWriter();
		r.bind("writer", writer) ;
		
		
		Object result = r.callFn("sample.hello", RhinoResponse.ReturnNative, 1, 2) ;
		assertEquals("Hello", writer.toString()) ;
		assertEquals("Hello", result.toString()) ;
	}
	
	
	public void testDirectCall() throws Exception {
		Scripter r = Scripter.create() ;
		StringWriter writer = new StringWriter();
		r.bind("writer", writer) ;
		r.directCall("sample", "writer.write('Hello') ; ") ;
		
		assertEquals("Hello", writer.toString()) ;
	}
	
	
	/*
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
		final Scripter rhiner = Scripter.create().start() ;
		String script = "new function(){"
				+ " this.hello = function(name) {return 'Hello ' + name; }, "
				+ " this.hi = function(greeting, name) {return greeting + ' ' + name; } "
				+ "} ;" ;
		
		rhiner.define("script", new StringReader(script)) ;
		String actual = rhiner.callFn("script.hi", String.class, "hi", "bleujin") ;
		assertEquals("hi bleujin", actual);
	}
	
	public void testBinding() throws Exception {
		final Scripter rhiner = Scripter.create().start() ;
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
		final Scripter rhiner = Scripter.create().start() ;
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
	*/
	

}
