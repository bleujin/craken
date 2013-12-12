package net.ion.script.rhino;


public class TestRhinoScript extends TestBaseScript{

    public void testDefineScript(){
        assertEquals(rengine.newScript("DefineScript").defineScript("print('Hello World!');").script(), "print('Hello World!');");
    }

    public void testFailure(){
    	Boolean result = rengine.newScript("Failure Script")
                .defineScript("failure")
                .exec(new ResponseHandler<Boolean>(){
					@Override
					public Boolean onFail(RhinoScript script, Throwable ex, long elapsedTime) {
						return Boolean.FALSE;
					}

					@Override
					public Boolean onSuccess(RhinoScript script, Object rtnValue, long elapsedTime) {
						return Boolean.TRUE ;
					}
                	
                });

        assertFalse(result);
    }

    public void testEmptyScript(){
    	RhinoResponse response = rengine.newScript("Empty Script").defineScript("").exec();
        assertTrue(response.isOk());
    }

    public void testNullScript(){
    	RhinoResponse response = rengine.newScript("Null Script").exec();
    	assertEquals(response.script(), "");
    }


}
