package net.ion.scriptexecutor.manager;

import net.ion.framework.util.MapUtil;
import net.ion.scriptexecutor.engine.ScriptEngine;
import net.ion.scriptexecutor.engine.GroovyEngine;
import net.ion.scriptexecutor.engine.RhinoEngine;
import net.ion.scriptexecutor.handler.ResponseHandler;
import net.ion.scriptexecutor.script.ScriptResponse;

import java.util.Map;

/**
 * Created with IntelliJ IDEA. User: Ryun Date: 2013. 9. 30. Time: 오전 10:20 To change this template use File | Settings | File Templates.
 */

public class ManagerBuilder {

	private LANG[] engines = { LANG.JAVASCRIPT, LANG.GROOVY };
	private Map<LANG, ScriptEngine> engineMap = MapUtil.newMap();
	private ResponseHandler<Object> responseHandler = new ResponseHandler<Object>() {
		@Override
		public Object success(ScriptResponse response) {
			return null;
		}

		@Override
		public Object failure(ScriptResponse response, Exception e) {
			return null;
		}
	};

	public enum LANG {
		JAVASCRIPT {
			protected RhinoEngine createEngine() {
				return new RhinoEngine();
			}

			protected Class<RhinoEngine> getEngineClass() {
				return RhinoEngine.class;
			}

		},
		GROOVY {
			protected GroovyEngine createEngine() {
				return new GroovyEngine();
			}

			protected Class<GroovyEngine> getEngineClass() {
				return GroovyEngine.class;
			}
		};

		protected abstract ScriptEngine createEngine();

		protected abstract Class<?> getEngineClass();
	}

	protected ManagerBuilder() {
	}

	public static final ManagerBuilder createBuilder() {
		return new ManagerBuilder();
	}

	public ScriptManager build() {
		for (ManagerBuilder.LANG lang : engines) {
			ScriptEngine engine = lang.createEngine();
			engineMap.put(lang, engine);
		}
		return ScriptManager.createManager(engineMap, responseHandler);
	}

	public ManagerBuilder languages(LANG... engines) {
		this.engines = engines;
		return this;
	}

	public ManagerBuilder responseHandler(ResponseHandler handler) {
		this.responseHandler = handler;
		return this;
	}

}
