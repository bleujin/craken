package net.ion.scriptexecutor.handler;


import org.mozilla.javascript.EvaluatorException;

/**
 * Created with IntelliJ IDEA.
 * User: Ryun
 * Date: 2013. 10. 1.
 * Time: 오후 1:49
 * To change this template use File | Settings | File Templates.
 */
public interface RhinoCompileHandler {

    void compileFailure(EvaluatorException e);
}
