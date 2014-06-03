package net.ion.script.rhino;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.framework.util.StringUtil;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;

public class Rhiner {

	private ExecutorService bossThread = Executors.newSingleThreadExecutor() ;
	private Context rootContext;
	private Map<String, Scriptable> packs = MapUtil.newMap();
	
	public static Rhiner create() {
		return new Rhiner();
	}

	public Rhiner start() {
		try {
			this.rootContext = bossThread.submit(new Callable<Context>(){
				@Override
				public Context call() throws Exception {
					Context context = Context.enter();
					return context;
				}
			}).get() ;
			
		} catch (InterruptedException e) {
			throw new IllegalStateException(e) ;
		} catch (ExecutionException e) {
			throw new IllegalStateException(e) ;
		}
		
		return this;
	}
	
	public Rhiner shutdown(){
		try {
			bossThread.submit(new Callable<Void>(){
				@Override
				public Void call() throws Exception {
					Context.exit();
					return null;
				}
			}).get() ;
		} catch (InterruptedException e) {
			throw new IllegalStateException(e) ;
		} catch (ExecutionException e) {
			throw new IllegalStateException(e) ;
		}
		
		bossThread.shutdown(); 
		return this ;
	}

	public Rhiner define(String name, String content) {
		Context context = Context.enter() ;
		try {
			ScriptableObject scope = context.initStandardObjects();
			Scriptable scriptable = (Scriptable) context.evaluateString(scope, content, name, 1, null);
			packs.put(name, scriptable) ;
		} finally {
			Context.exit(); 
		}
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
		ScriptableObject scope = rootContext.initStandardObjects();
		
		String[] names = StringUtil.split(fullName, '.') ;
		Scriptable scriptable = packs.get(names[0]) ;
		if (scriptable == null) rhinerHandler.onFail(new IllegalArgumentException("not found : " + fullName)) ;
		
		Object prop = scriptable.get(names[1], null) ;
		if (prop == null) rhinerHandler.onFail(new IllegalArgumentException("not found : " + fullName)) ;
		
		Object result = ((Function)prop).call(rootContext, scriptable, scope, params)  ;
		return rhinerHandler.onSuccess(result) ;
	}

}
