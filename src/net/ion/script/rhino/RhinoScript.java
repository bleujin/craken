package net.ion.script.rhino;

import java.io.IOException;
import java.io.Reader;
import java.util.Map;

import net.ion.framework.util.IOUtil;
import net.ion.framework.util.MapUtil;

import org.mozilla.javascript.edu.emory.mathcs.backport.java.util.Collections;

public class RhinoScript {

	private final RhinoEngine engine ;
	private final String name;
	private String script = "";
	private Map<String, Object> bindings = MapUtil.newMap() ;
	
	RhinoScript(RhinoEngine engine, String name) {
		this.engine = engine ;
		this.name = name ;
	}

	public RhinoScript defineScript(String script) {
		this.script = script ;
		return this;
	}
	
	public RhinoScript defineScript(Reader reader) throws IOException {
		this.script = IOUtil.toStringWithClose(reader) ;
		return this;
	}


	public RhinoResponse exec() {
		return exec(ResponseHandler.DEFAULT);
	}

	public <T> T exec(ResponseHandler<T> rhandler) {
		return engine.run(this, rhandler);
	}

	
	public Map<String, Object> bindings() {
		return Collections.unmodifiableMap(bindings);
	}

	public RhinoScript bind(String key, Object value) {
		bindings.put(key, value) ;
		return this;
	}

	public String script() {
		return script;
	}

	public String name() {
		return name;
	}


}
