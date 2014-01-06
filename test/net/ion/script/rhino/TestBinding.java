package net.ion.script.rhino;


public class TestBinding extends TestBaseScript {

	public void testBind() {
		RhinoResponse response = rengine.newScript("Bind Script").defineScript("bindTest;").bind("bindTest", "Hello World!").exec();

		assertEquals(response.getReturn(String.class), "Hello World!");
	}

	public void testBindNull() {
		RhinoResponse response = rengine.newScript("Null Bind Script").defineScript("print(bindTest); bindTest").bind("bindTest", null).exec();

		assertTrue(response.isOk());
		assertNull(response.getReturn(Object.class)) ;
	}

	public void testNotDefine() {
		Boolean result = rengine.newScript("main Script").defineScript("print(str);").exec(ResponseHandler.FalseOnError);
		assertEquals(Boolean.FALSE, result);
	}

	public void testPreScriptOverrideBinding() {
		RhinoResponse response = rengine.newScript("overwrite")
			.defineScript("print(bvalue); bvalue ;")
			.bind("bvalue", "preScript")
			.bind("bvalue", "afterScript").exec();
		
		assertEquals(response.getReturn(String.class), "afterScript");
	}

}
