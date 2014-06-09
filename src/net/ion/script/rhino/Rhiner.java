package net.ion.script.rhino;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

public class Rhiner {

	private Context rootContext;
	private Map<String, Scriptable> packs = MapUtil.newMap();
	private ScriptableObject sharedScope;
	
	public static Rhiner create() {
		return new Rhiner();
	}

	public Rhiner start() {
		Context context = Context.enter();
		Global global = new Global(context);

		global.setOut(System.out);
		global.setErr(System.err);
		
		Rhiner.this.sharedScope = context.initStandardObjects();
		sharedScope.setPrototype(global);
					
		
		this.rootContext = context ;
		return this;
	}
	
	public Rhiner shutdown(){
		Context.exit();

		return this ;
	}

	public Rhiner define(final String name, final String content) {
		Scriptable scope = rootContext.newObject(sharedScope);
		Scriptable scriptable = (Scriptable) rootContext.evaluateString(scope, content, name, 1, null);
		packs.put(name, scriptable) ;

		return this ;
	}
	
	public Rhiner define(String name, Reader content) throws IOException {
		return define(name, IOUtil.toStringWithClose(content)) ; 
	}

	
	public <T> T callFn(String fullName, final Class<T> returnType, Object... params) {
		return callFn(fullName, new RhinerHandler<T>() {
			@Override
			public T onSuccess(Object result) {
				return (T) Context.jsToJava(result, returnType);
			}

			@Override
			public T onFail(Exception ex) {
				return null;
			}
		}, params) ;
	}

	
	public <T> T callFn(String fullName, RhinerHandler<T> rhinerHandler, Object... params) {
		Scriptable fnScope = rootContext.initStandardObjects() ;

		String[] names = StringUtil.split(fullName, '.') ;
		Scriptable scriptable = packs.get(names[0]) ;
		if (scriptable == null) rhinerHandler.onFail(new IllegalArgumentException("not found : " + fullName)) ;


		Object prop = scriptable.get(names[1], scriptable) ;
		if (prop == null) rhinerHandler.onFail(new IllegalArgumentException("not found : " + fullName)) ;
		
		Object result = ((Function)prop).call(rootContext, scriptable, fnScope, params)  ;
		return rhinerHandler.onSuccess(result) ;
	}

	public void bind(String name, Object value) {
		sharedScope.put(name, sharedScope, value);
	}

}
