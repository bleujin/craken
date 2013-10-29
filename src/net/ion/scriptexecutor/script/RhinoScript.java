package net.ion.scriptexecutor.script;

import net.ion.scriptexecutor.engine.AbScript;
import net.ion.scriptexecutor.engine.ScriptEngine;
import net.ion.scriptexecutor.engine.RhinoEngine;
import net.ion.scriptexecutor.handler.ResponseHandler;
import net.ion.scriptexecutor.handler.RhinoCompileHandler;
import net.ion.scriptexecutor.manager.ManagerBuilder;
import net.ion.scriptexecutor.manager.ScriptManager;
import org.mozilla.javascript.EvaluatorException;
import org.mozilla.javascript.Script;

/**
 * Created with IntelliJ IDEA. User: Ryun Date: 2013. 9. 30. Time: 오전 10:20 To change this template use File | Settings | File Templates.
 */
public class RhinoScript extends AbScript<RhinoScript> {

	public RhinoScript(String name, ManagerBuilder.LANG language, ScriptEngine engine, ScriptManager manager) {
		super(name, language, engine, manager);
	}

	@Override
	public ScriptResponse execute(ResponseHandler<ScriptResponse> handler) {
		ScriptResponse scriptResponse = super.run(this);
		if (scriptResponse.isSuccess())
			return handler.success(scriptResponse);
		else
			return handler.failure(scriptResponse, scriptResponse.exception());
	}

	public ScriptResponse execute() {
		return super.run(this);
	}

	public Script setPreScript(RhinoCompileHandler handler) {

		try {
			return ((RhinoEngine) engine).addPredefinedScript(this);
		} catch (EvaluatorException e) {
			handler.compileFailure(e);
		}

		return null;
	}

	public Script setPreScript() {
		try {
			return ((RhinoEngine) engine).addPredefinedScript(this);
		} catch (EvaluatorException e) {
			e.printStackTrace();
		}

		return null;
	}

}
