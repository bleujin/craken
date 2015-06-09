package net.ion.script.rhino;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

public class Scripter {

	private Map<String, Object> packs = MapUtil.newMap();
	
	private ScriptEngine engine ;
	private Scripter(ScriptEngine engine){
		this.engine = engine ;
	}
	
	public static Scripter create() {
		ScriptEngine engine = new ScriptEngineManager().getEngineByName("JavaScript") ;
		return new Scripter(engine);
	}

	public Scripter start() {
		return this;
	}
	
	public Scripter shutdown(){
		return this ;
	}

	public Scripter define(final String name, final String content) throws ScriptException {
		if (packs.containsKey(name)) throw new IllegalArgumentException("already exist script : " + name) ;
		packs.put(name, engine.eval(content)) ;
		
		return this ;
	}
	
	public Scripter define(String name, Reader content) throws ScriptException, IOException {
		return define(name, IOUtil.toStringWithClose(content)) ; 
	}
	
	public Object directCall(String name, String script) throws ScriptException{
		String pcontent = "new function(){"
				+ " this.call = function(){ " + script + " }"
				+ "} " ;
		Object pack = engine.eval(pcontent) ;

		RhinoResponse rhinerHandler = RhinoResponse.ReturnNative ;
		try {
			Object result = ((Invocable)engine).invokeMethod(pack, "call");
			return rhinerHandler.onSuccess("call", new Object[0], result) ;
		} catch (ScriptException e) {
			return rhinerHandler.onThrow("call", new Object[0], e) ;
		} catch (NoSuchMethodException e) {
			return rhinerHandler.onThrow("call", new Object[0], e) ;
		}

	}

	
	public <T> T callFn(String fullName, RhinoResponse<T> rhinerHandler, Object... params) {
		String[] names = StringUtil.split(fullName, '.') ;
		
		Object pack = packs.get(names[0]) ;
		if (pack == null) rhinerHandler.onThrow(fullName, params, new IllegalArgumentException("not found package : " + names[0])) ;

		try {
			Object result = ((Invocable)engine).invokeMethod(pack, names[1], params);
			return rhinerHandler.onSuccess(fullName, params, result) ;
		} catch (ScriptException e) {
			return rhinerHandler.onThrow(fullName, params, e) ;
		} catch (NoSuchMethodException e) {
			return rhinerHandler.onThrow(fullName, params, e) ;
		}
	}

	
	public Scripter bind(String name, Object value) {
		engine.put(name, value);
		return this ;
	}

}
