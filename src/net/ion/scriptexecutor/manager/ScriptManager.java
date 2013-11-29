package net.ion.scriptexecutor.manager;

import java.util.Map;

import net.ion.framework.util.MapUtil;
import net.ion.scriptexecutor.engine.AbScript;
import net.ion.scriptexecutor.engine.ScriptEngine;
import net.ion.scriptexecutor.handler.ResponseHandler;
import net.ion.scriptexecutor.script.GroovyScript;
import net.ion.scriptexecutor.script.RhinoScript;
import net.ion.scriptexecutor.script.ScriptResponse;

/**
 * Created with IntelliJ IDEA.
 * User: Ryun
 * Date: 2013. 9. 30.
 * Time: 오전 10:28
 * To change this template use File | Settings | File Templates.
 */
public class ScriptManager {

    private Map<ManagerBuilder.LANG, ScriptEngine> engineMap = MapUtil.newMap();
    private ResponseHandler handler;

    public ScriptManager(Map<ManagerBuilder.LANG, ScriptEngine> engineMap) {
        this.engineMap = engineMap;
    }

    public ScriptManager(Map<ManagerBuilder.LANG, ScriptEngine> engineMap, ResponseHandler handler) {
        this.engineMap = engineMap;
        this.handler = handler;
    }

    protected static ScriptManager createManager(Map<ManagerBuilder.LANG, ScriptEngine> engineMap, ResponseHandler handler){
        return new ScriptManager(engineMap, handler);
    }

    protected static ScriptManager createManager(Map<ManagerBuilder.LANG, ScriptEngine> engineMap){
        return new ScriptManager(engineMap);
    }

    public void start(){
        for(ScriptEngine engine : engineMap.values()){
            engine.start();
        }
    }

    public void shutdown(){
        for(ScriptEngine engine : engineMap.values()){
            engine.shutdown();
        }
    }

    public <T extends ScriptEngine> T getEngine(Class<T> cls){

        for(ScriptEngine engine : engineMap.values())
            if(engine.getClass() == cls)
                return (T) engine;

        return null;
    }

    public RhinoScript createRhinoScript(String name){
        return new RhinoScript(name, ManagerBuilder.LANG.JAVASCRIPT, engineMap.get(ManagerBuilder.LANG.JAVASCRIPT), this);
    }

    public GroovyScript createGroovyScript(String name){
        return new GroovyScript(name, ManagerBuilder.LANG.GROOVY, engineMap.get(ManagerBuilder.LANG.JAVASCRIPT), this);
    }

    protected ScriptResponse execute(final AbScript script){
        return execute(script, this.handler);
    }

    protected ScriptResponse execute(final AbScript script, final ResponseHandler handler){
        return null;
    }




}
