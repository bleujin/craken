package net.ion.script.rhino.engine;

import java.util.Vector;

import junit.framework.TestCase;

import net.ion.script.rhino.DebugWrapFactory;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.NativeJavaMethod;
import org.mozilla.javascript.NativeJavaObject;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.WrapFactory;

public class TestMethodWrapper extends TestCase {

	public void testInter() throws Exception {
		Context cx = Context.enter();
		cx.setWrapFactory(new DebugWrapFactory());
		Scriptable root = cx.initStandardObjects();
		ScriptableObject.putProperty(root, "v", new Vector<String>());
		cx.evaluateString(root, "v.add('foo'); v.get(0)", "src", 1, null);
	}
}
