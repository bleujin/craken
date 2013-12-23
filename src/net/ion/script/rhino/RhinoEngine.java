package net.ion.script.rhino;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.ListUtil;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.ContextFactory;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

public class RhinoEngine  {

	private ScriptableObject sharedScope;
	private PrintStream outPrint = System.out;
	private List<PreCompiledScript> preScripts = ListUtil.newList();
	private boolean isStarted = false;
	private ExecutorService bossThread = Executors.newSingleThreadExecutor();
	
	static {
		ContextFactory.initGlobal(new CustomContextFactory());
	}
	
	private RhinoEngine() {
	}

	public static RhinoEngine create() throws IOException {
		RhinoEngine result = new RhinoEngine();
		return result;
	}

	public static RhinoEngine createWithJQuery() throws IOException {
		RhinoEngine result = new RhinoEngine();

		result.preDefineScript("envjs", IOUtil.toStringWithClose(RhinoEngine.class.getResourceAsStream("env.rhino.1.2.js")));
		result.preDefineScript("jquery", IOUtil.toStringWithClose(RhinoEngine.class.getResourceAsStream("jquery-1.10.2.min.js")));

		return result;
	}

	public RhinoEngine preDefineScript(String name, String script) {
		if (isStarted)
			throw new IllegalStateException("cannt define prescript, cause already started. ");
		RhinoScript pscript = newScript(name).defineScript(script);
		try {
			Context context = Context.enter();
			Script compiledScript = context.compileString(pscript.scriptCode(), pscript.name(), 1, null);
			preScripts.add(PreCompiledScript.create(compiledScript, pscript));
		} finally {
			Context.exit();
		}
		return this;
	}

	public RhinoScript newScript(String name) {
		return new RhinoScript(this, name);
	}

	public RhinoEngine outPrint(PrintStream out) {
		this.outPrint = out;
		return this;
	}

	public Future<RhinoEngine> start() {
		return bossThread.submit(new Callable<RhinoEngine>() {
			@Override
			public RhinoEngine call() throws Exception {
				if (isStarted)
					return RhinoEngine.this;

				Context engineContext = Context.enter();

				Global global = new Global(engineContext);

				global.setOut(outPrint);
				global.setErr(System.err);
				

				sharedScope = engineContext.initStandardObjects();
				sharedScope.setPrototype(global);

				Scriptable argsObj = engineContext.newArray(sharedScope, new Object[] {});
				sharedScope.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);

				for (PreCompiledScript preScript : preScripts) {
					for (Entry<String, Object> entry : preScript.bindings().entrySet()) {
						sharedScope.put(entry.getKey(), sharedScope, entry.getValue());
					}
					preScript.exec(engineContext, sharedScope);
				}
				RhinoEngine.this.isStarted = true;

				return RhinoEngine.this;
			}
		});
	}

	public <T> T run(RhinoScript script, ResponseHandler<T> rhandler) {
		if (!isStarted)
			throw new IllegalStateException("not started");

		long start = System.currentTimeMillis();

		try {
			Context context = Context.enter();
			context.getWrapFactory().setJavaPrimitiveWrap(false);
			context.setInstructionObserverThreshold(50000);

			Scriptable scope = context.newObject(sharedScope);
			scope.setPrototype(sharedScope);
			scope.setParentScope(null);
			for (Entry<String, Object> entry : script.bindings().entrySet()) {
				scope.put(entry.getKey(), scope, entry.getValue());
			}

			Object obj = context.evaluateString(scope, script.scriptCode(), script.name(), 1, null);
			return rhandler.onSuccess(script, obj, System.currentTimeMillis() - start);
		} catch (Throwable e) {
			return rhandler.onFail(script, e, System.currentTimeMillis() - start);

		} finally {
			Context.exit();
		}
	}

	public void shutdown() {
		try {
			bossThread.submit(new Callable<Void>() {
				@Override
				public Void call() {
					if (outPrint != System.out)
						IOUtil.closeQuietly(outPrint);
					Context.exit();

					return null;
				}
			}).get();
			bossThread.shutdown();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

}

class PreCompiledScript {

	private Script script;
	private RhinoScript rscript;

	public PreCompiledScript(Script script, RhinoScript rscript) {
		this.script = script;
		this.rscript = rscript;
	}

	public void exec(Context context, ScriptableObject scriptable) {
		script.exec(context, scriptable);
	}

	public Map<String, Object> bindings() {
		return rscript.bindings();
	}

	public static PreCompiledScript create(Script script, RhinoScript rscript) {
		return new PreCompiledScript(script, rscript);
	}

}

class CustomContextFactory extends ContextFactory {
	// Custom Context to store execution time.
	private static class CustomContext extends Context {
		long startTime;
	}



	// Override makeContext()
	protected Context makeContext() {
		CustomContext cx = new CustomContext();
		cx.setInstructionObserverThreshold(50000); // Make Rhino runtime to call observeInstructionCount each 10000 bytecode instructions
		return cx;
	}

	// Override hasFeature(Context, int)
	public boolean hasFeature(Context cx, int featureIndex) {
		
		switch (featureIndex) { // Turn on maximum compatibility with MSIE scripts
		case Context.FEATURE_NON_ECMA_GET_YEAR:
			return true;
		case Context.FEATURE_MEMBER_EXPR_AS_FUNCTION_NAME:
			return true;
		case Context.FEATURE_RESERVED_KEYWORD_AS_IDENTIFIER:
			return true;
		case Context.FEATURE_PARENT_PROTO_PROPERTIES:
			return false;
		case Context.FEATURE_DYNAMIC_SCOPE :
			return true;
			
		}
		return super.hasFeature(cx, featureIndex);
	}

	
	private final int MAX_TIMEOUT = 10000 ;
	@Override
	protected void observeInstructionCount(Context cx, int instructionCount) {
		CustomContext mcx = (CustomContext) cx;
		long currentTime = System.currentTimeMillis();
		if (currentTime - mcx.startTime > MAX_TIMEOUT) {
			throw new Error();
		}
	}

	@Override
	protected Object doTopCall(org.mozilla.javascript.Callable ca, Context cx, Scriptable scope, Scriptable thisObj, Object[] args) {
		CustomContext mcx = (CustomContext) cx;
		mcx.startTime = System.currentTimeMillis();
		return super.doTopCall(ca, mcx, scope, thisObj, args) ;
	}
}
