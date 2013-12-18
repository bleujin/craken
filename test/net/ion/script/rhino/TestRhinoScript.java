package net.ion.script.rhino;


public class TestRhinoScript extends TestBaseScript{

    public void testDefineScript(){
        assertEquals(rengine.newScript("DefineScript").defineScript("print('Hello World!');").scriptCode(), "print('Hello World!');");
    }

    public void testFailure(){
    	Boolean result = rengine.newScript("FailureScript")
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
    	RhinoResponse response = rengine.newScript("EmptyScript").defineScript("").exec();
        assertTrue(response.isOk());
    }

    public void testNullScript(){
    	RhinoResponse response = rengine.newScript("NullScript").exec();
    	assertEquals(response.script(), "");
    }

    
    

}
