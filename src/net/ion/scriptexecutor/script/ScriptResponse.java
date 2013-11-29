package net.ion.scriptexecutor.script;

import java.io.PrintStream;
import java.util.Date;

import net.ion.scriptexecutor.engine.AbScript;

/**
 * Created with IntelliJ IDEA. User: Ryun Date: 2013. 9. 30. Time: 오전 10:36 To change this template use File | Settings | File Templates.
 */

public class ScriptResponse {

	private Date executionTime;
	private String printed;
	private boolean success;
	private AbScript script;
	private Exception e;
	private Object obj;

	private ScriptResponse(AbScript script, boolean success, String printed, Object obj, Exception e) {
		this.script = script;
		this.success = success;
		this.printed = printed;
		this.executionTime = new Date();
		this.obj = obj;
		this.e = e;
	}

	public static ScriptResponse createSuccess(AbScript script, String printed) {
		return new ScriptResponse(script, true, printed, null, null);
	}

	public static ScriptResponse createSuccess(AbScript script, String printed, Object obj) {
		return new ScriptResponse(script, true, printed, obj, null);
	}

	public static ScriptResponse createFailure(AbScript script, String printed, Exception e) {
		return new ScriptResponse(script, false, printed, null, e);
	}

	public <T> T getObject(Class<T> clazz) {
		return (T) obj;
	}

	public boolean isSuccess() {
		return success;
	}

	public Exception exception() {
		return e;
	}

	public String printed() {
		return printed;
	}

	public AbScript script() {
		return script;
	}

	public void print(PrintStream out) {
		out.println(printed) ;
	}

}
