package net.ion.scriptexecutor.engine;

import net.ion.scriptexecutor.script.ScriptResponse;

/**
 * Created with IntelliJ IDEA. User: Ryun Date: 2013. 9. 30. Time: 오전 10:57 To change this template use File | Settings | File Templates.
 */
public interface ScriptEngine {

	public void start();

	public void shutdown();

	ScriptResponse run(AbScript script);

}
