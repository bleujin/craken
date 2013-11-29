package net.ion.scriptexecutor.engine;

import java.io.IOException;
import java.io.Reader;
import java.util.Collections;
import java.util.Map;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;
import net.ion.scriptexecutor.handler.ResponseHandler;
import net.ion.scriptexecutor.manager.ManagerBuilder;
import net.ion.scriptexecutor.manager.ScriptManager;
import net.ion.scriptexecutor.script.ScriptResponse;

/**
 * Created with IntelliJ IDEA. User: Ryun Date: 2013. 9. 30. Time: 오전 10:20 To change this template use File | Settings | File Templates.
 */
public abstract class AbScript<T extends AbScript<T>> {

	private String name;
	protected ManagerBuilder.LANG language;
	protected String code = "";
	protected ScriptManager manager;
	protected ScriptEngine engine;
	protected Map<String, Object> bind = MapUtil.newMap();

	public AbScript(String name, ManagerBuilder.LANG language, ScriptEngine engine, ScriptManager manager) {
		this.name = name;
		this.engine = engine;
		this.language = language;
		this.manager = manager;
	}

	public T defineScript(String code) {
		this.code = code;
		return (T) this;
	}

	public T defineScript(Reader reader) throws IOException {
		this.code = IOUtil.toStringWithClose(reader);
		return (T) this;
	}

	public ManagerBuilder.LANG language() {
		return language;
	}

	public String code() {
		return code;
	}

	public String name() {
		return name;
	}

	protected T setEngine(ScriptEngine engine) {
		this.engine = engine;
		return (T) this;
	}

	public Map<String, Object> getBind() {
		return Collections.unmodifiableMap(bind);
	}

	public T bind(String key, Object value) {
		bind.put(key, value);
		return (T) this;
	}

	public ScriptResponse run(AbScript absScript) {
		return engine.run(absScript);
	}

	abstract public ScriptResponse execute(ResponseHandler<ScriptResponse> handler);
}
