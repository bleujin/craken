package net.ion.script.rhino;

import java.io.IOException;

public class TestJavascriptFunction extends TestBaseScript {

	public void testJavaScriptFunction() {
		Boolean result = rengine.newScript("Javascript Function Main Script").defineScript("load(\"functionTest\");").exec(ResponseHandler.FalseOnError);
		assertEquals(Boolean.FALSE, result) ;
	}

	public void testEvalException() {
		rengine.newScript("test Function").defineScript("print('a);").exec(new ResponseHandler<Void>() {
			@Override
			public Void onFail(RhinoScript script, Throwable ex, long elapsedTime) {
				assertEquals(ex.getMessage(), "unterminated string literal (test Function#1)");
				return null;
			}

			@Override
			public Void onSuccess(RhinoScript script, Object rtnValue, long elapsedTime) {
				return null;
			}
		});
	}

	public void testExternalScript() throws IOException {

		Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
			@Override
			public void uncaughtException(Thread thread, Throwable throwable) {
				throwable.printStackTrace();
			}
		});

		rengine.newScript("main js").defineScript("console.log(window);").exec();

	}
}
