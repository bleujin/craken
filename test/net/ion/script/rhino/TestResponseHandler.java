package net.ion.script.rhino;


public class TestResponseHandler extends TestBaseScript {
	
	public void testElapsedTime() throws Exception {
		RhinoResponse response = rengine.newScript("Hello").defineScript("print('Hello World');").exec();
		long etime = response.elapsedTime() ;
		assertEquals(true, etime < 10) ;
	}
	
	public void testHandlerOnFail() throws Exception {
		String result = rengine.newScript("Hello").defineScript("printHello('Hello World');").exec(new ResponseHandler<String>(){
			public String onFail(RhinoScript script, Throwable ex, long elapsedTime) {
				return "Fail";
			}
			public String onSuccess(RhinoScript script, Object rtnValue, long elapsedTime) {
				return "Success";
			}
		});
		assertEquals("Fail", result) ;
	}
}
