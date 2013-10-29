package net.ion.scriptexecutor.script;

import net.ion.scriptexecutor.engine.AbScript;
import net.ion.scriptexecutor.engine.ScriptEngine;
import net.ion.scriptexecutor.handler.ResponseHandler;
import net.ion.scriptexecutor.manager.ManagerBuilder;
import net.ion.scriptexecutor.manager.ScriptManager;

/**
 * Created with IntelliJ IDEA. User: Ryun Date: 2013. 9. 30. Time: 오전 11:06 To change this template use File | Settings | File Templates.
 */
public class GroovyScript extends AbScript<GroovyScript> {

	public GroovyScript(String name, ManagerBuilder.LANG language, ScriptEngine engine, ScriptManager manager) {
		super(name, language, engine, manager);
	}

	@Override
	public ScriptResponse execute(ResponseHandler<ScriptResponse> handler) {
		return null;
	}

}
