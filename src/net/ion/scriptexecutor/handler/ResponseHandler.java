package net.ion.scriptexecutor.handler;

import net.ion.scriptexecutor.script.ScriptResponse;

/**
 * Created with IntelliJ IDEA.
 * User: Ryun
 * Date: 2013. 9. 6.
 * Time: 오후 3:05
 * To change this template use File | Settings | File Templates.
 */
public interface ResponseHandler<T>{
    public T success(ScriptResponse response);
    public T failure(ScriptResponse response, Exception e);
}
