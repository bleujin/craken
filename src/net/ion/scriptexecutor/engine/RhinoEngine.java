package net.ion.scriptexecutor.engine;

import net.ion.framework.util.MapUtil;
import net.ion.scriptexecutor.script.RhinoScript;
import net.ion.scriptexecutor.script.ScriptResponse;
import org.mozilla.javascript.*;
import org.mozilla.javascript.tools.shell.Global;

import java.io.*;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: Ryun Date: 2013. 9. 30. Time: 오전 11:09 To change this template use File | Settings | File Templates.
 */

public class RhinoEngine extends ContextFactory implements ScriptEngine {

	private Map<Script, RhinoScript> preScriptList = MapUtil.newOrdereddMap();
	private Map<String, Object> bind = MapUtil.newMap();
	private ScriptableObject sharedScope;
	private boolean useDynamicScope;
	private Global global;
	private PrintStream printStream;

	static {
		ContextFactory.initGlobal(new RhinoEngine());
	}

	private ByteArrayOutputStream bais;

	@Override
	protected boolean hasFeature(Context cx, int featureIndex) {

		if (featureIndex == Context.FEATURE_DYNAMIC_SCOPE) {
			return useDynamicScope;
		}

		return super.hasFeature(cx, featureIndex);
	}

	public static Object print(Context cx, Scriptable thisObj, Object[] args, Function funObj) {
		PrintStream out = System.out;
		for (int i = 0; i < args.length; i++) {
			if (i > 0)
				out.print(" ");

			// Convert the arbitrary JavaScript value into a string form.
			String s = Context.toString(args[i]);

			out.print(s);
		}
		out.println();
		return Context.getUndefinedValue();
	}

	@Override
	public void start() {

		Context engineContext = Context.enter();

		global = new Global(engineContext);

		try {
			bais = new ByteArrayOutputStream();
			printStream = new PrintStream(bais, true, "UTF-8");
		} catch (UnsupportedEncodingException e) {
		}

		global.setOut(printStream);
		global.setOut(printStream);

		try {
			sharedScope = engineContext.initStandardObjects();
			sharedScope.setPrototype(global);

			Scriptable argsObj = engineContext.newArray(sharedScope, new Object[] {});
			sharedScope.defineProperty("arguments", argsObj, ScriptableObject.DONTENUM);

			useDynamicScope = true;

			for (Script preScript : preScriptList.keySet()) {
				Map<String, Object> preScriptBindMap = preScriptList.get(preScript).getBind();
				for (String key : preScriptBindMap.keySet()) {
					sharedScope.put(key, sharedScope, preScriptBindMap.get(key));
				}
				preScript.exec(engineContext, sharedScope);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	public void shutdown() {
		Context.exit();
	}

	@Override
	public ScriptResponse run(AbScript script) {

		// Create Context;
		Context context = Context.enter();
		ScriptResponse scriptResponse;

		Scriptable scope = context.newObject(sharedScope);
		scope.setPrototype(sharedScope);
		scope.setParentScope(null);

		try {

			for (Object key : script.getBind().keySet()) {
				scope.put(key.toString(), scope, script.getBind().get(key));
			}

			Object obj = context.evaluateString(scope, script.code(), script.name(), 1, null);
			scriptResponse = ScriptResponse.createSuccess(script, bais.toString(), obj);
			bais.reset();

		} catch (Exception e) {
			e.printStackTrace(printStream);
			scriptResponse = ScriptResponse.createFailure(script, bais.toString(), e);
			bais.reset();

		} finally {

			// Exit context;
			Context.exit();
		}

		return scriptResponse;
	}

	public Script addPredefinedScript(RhinoScript script) {
		Script compiledScript = null;
		try {
			Context context = Context.enter();

			compiledScript = context.compileString(script.code(), script.name(), 1, null);
			preScriptList.put(compiledScript, script);

			Context.exit();
		} catch (Exception e) {
			e.printStackTrace();
		}

		return compiledScript;
	}

	public void bind(String key, Object obj) {
		bind.put(key, obj);
	}

	public Map<Script, RhinoScript> getPreScriptList() {
		return preScriptList;
	}

}
