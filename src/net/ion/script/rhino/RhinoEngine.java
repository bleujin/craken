package net.ion.script.rhino;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

public class RhinoEngine extends ContextFactory {
	
	private ScriptableObject sharedScope;
	private PrintStream outPrint;
	private List<PreCompiledScript> preScripts = ListUtil.newList() ; 
	
	private RhinoEngine(){
	}

	public static RhinoEngine create() throws IOException {
		RhinoEngine result = new RhinoEngine();
		return result ;
	}

	public static RhinoEngine createWithJQuery() throws IOException {
		RhinoEngine result = new RhinoEngine();
		
		result.definePreScript("envjs", IOUtil.toStringWithClose(RhinoEngine.class.getResourceAsStream("env.rhino.1.2.js"))) ;
		result.definePreScript("jquery", IOUtil.toStringWithClose(RhinoEngine.class.getResourceAsStream("jquery-1.10.2.min.js"))) ;
		
		return result ;
	}

	public RhinoEngine definePreScript(String name, String script) {
		RhinoScript pscript = newScript(name).defineScript(script);
		try {
			Context context = Context.enter();
			Script compiledScript = context.compileString(pscript.scriptCode(), pscript.name(), 1, null);
			preScripts.add(PreCompiledScript.create(compiledScript, pscript));
		} finally {
			Context.exit();
		}

		return this ;
	}


	public RhinoScript newScript(String name) {
		return new RhinoScript(this, name);
	}

	
	@Override
	protected boolean hasFeature(Context cx, int featureIndex) {
		if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE) {
			return true;
		}

		return super.hasFeature(cx, featureIndex);
	}
	
	private boolean isStarted = false ;
	public RhinoEngine start() {
		if (isStarted) return this ;
		
		Context engineContext = Context.enter();

		Global global = new Global(engineContext);

		outPrint = System.out ;
		global.setOut(outPrint);
		global.setErr(System.err);

		sharedScope = engineContext.initStandardObjects();
		sharedScope.setPrototype(global);

		Scriptable argsObj = engineContext.newArray(sharedScope, new Object[] {});
		sharedScope.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);

		for (PreCompiledScript cscript : preScripts) {
			for (Entry<String, Object> entry : cscript.bindings().entrySet()) {
				sharedScope.put(entry.getKey(), sharedScope, entry.getValue());
			}
			cscript.exec(engineContext, sharedScope);
		}
		this.isStarted = true ;
		return this ;
	}
	
	
	
	public <T> T run(RhinoScript script, ResponseHandler<T> rhandler) {
		start() ;
		
		long start = System.currentTimeMillis() ;
		Context context = Context.enter();

		Scriptable scope = context.newObject(sharedScope);
		scope.setPrototype(sharedScope);
		scope.setParentScope(null);

		try {
			for (Entry<String, Object> entry : script.bindings().entrySet()) {
				scope.put(entry.getKey(), scope, entry.getValue());
			}

			Object obj = context.evaluateString(scope, script.scriptCode(), script.name(), 1, null);
			return rhandler.onSuccess(script, obj, System.currentTimeMillis() - start) ;
		} catch (Throwable e) {
			return rhandler.onFail(script, e, System.currentTimeMillis() - start) ; 

		} finally {
			Context.exit();
		}
	}

	public void close() {
		IOUtil.close(outPrint) ;
		Context.exit() ;
	}

}

class PreCompiledScript {

	private Script script;
	private RhinoScript rscript;

	public PreCompiledScript(Script script, RhinoScript rscript) {
		this.script = script ;
		this.rscript = rscript ;
	}

	public void exec(Context context, ScriptableObject scriptable) {
		script.exec(context, scriptable) ;
	}

	public Map<String, Object> bindings() {
		return rscript.bindings();
	}

	public static PreCompiledScript create(Script script, RhinoScript rscript) {
		return new PreCompiledScript(script, rscript);
	}
	
}



