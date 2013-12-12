package net.ion.script.rhino.engine;

import java.util.Vector;

import junit.framework.TestCase;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;

public class TestMethodWrapper extends TestCase{

	
	public void testInter() throws Exception {
		Context cx=Context.enter();
        cx.setWrapFactory(new InterceptWrapFactory());
        Scriptable root=cx.initStandardObjects();
        ScriptableObject.putProperty(root,"v", new Vector<String>());
        cx.evaluateString(root, "v.add('foo'); v.get(0)", "src", 1, null);
	}
}


class InterceptWrapFactory extends WrapFactory{
    @Override
    public Scriptable wrapAsJavaObject(Context cx, Scriptable scope, Object javaObject, Class<?> staticType) {
        return new InterceptNativeObject(scope, javaObject, staticType);
    }
}

class InterceptNativeObject extends NativeJavaObject {
    @Override
    public Object get(String name, Scriptable start) {
        Object res = super.get(name, start);
        System.out.println("Field get name="+name+" result="+res);
        if (res instanceof NativeJavaMethod) {
            NativeJavaMethod method = (NativeJavaMethod) res;
            return new JavaMethodWrapper(method);
        }
        return res;
    }
    public InterceptNativeObject(Scriptable scope, Object javaObject,
            Class<?> staticType) {
        super(scope, javaObject, staticType);
    }
}

class JavaMethodWrapper implements Function {
    NativeJavaMethod method;
    public JavaMethodWrapper(NativeJavaMethod method) {
        this.method = method;
    }
    public boolean hasInstance(Scriptable instance) {
        return method.hasInstance(instance);
    }
    public Object call(Context cx, Scriptable scope, Scriptable thisObj,
            Object[] args) {
        System.out.println("Call method: "+method);
        return method.call(cx, scope, thisObj, args);
    }
    public boolean has(int index, Scriptable start) {
        return method.has(index, start);
    }
    public Scriptable construct(Context cx, Scriptable scope, Object[] args) {
        return method.construct(cx, scope, args);
    }
    public void put(int index, Scriptable start, Object value) {
        method.put(index, start, value);
    }
    public void delete(int index) {
        method.delete(index);
    }
    public Scriptable createObject(Context cx, Scriptable scope) {
        return method.createObject(cx, scope);
    }
    public boolean has(String name, Scriptable start) {
        return method.has(name, start);
    }
    public void defineConst(String name, Scriptable start) {
        method.defineConst(name, start);
    }
    public void put(String name, Scriptable start, Object value) {
        method.put(name, start, value);
    }
    public void delete(String name) {
        method.delete(name);
    }
    public Scriptable getPrototype() {
        return method.getPrototype();
    }
    public void setPrototype(Scriptable m) {
        method.setPrototype(m);
    }
    public Scriptable getParentScope() {
        return method.getParentScope();
    }
    public void setParentScope(Scriptable m) {
        method.setParentScope(m);
    }
    public Object[] getIds() {
        return method.getIds();
    }
    public Object get(int index, Scriptable start) {
        return method.get(index, start);
    }
    public Object get(String name, Scriptable start) {
        return method.get(name, start);
    }
    public String getClassName() {
        return method.getClassName();
    }
    public Object getDefaultValue(Class<?> typeHint) {
        return method.getDefaultValue(typeHint);
    }
}
