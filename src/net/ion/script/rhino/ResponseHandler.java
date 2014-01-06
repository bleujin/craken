package net.ion.script.rhino;

public abstract class ResponseHandler<T> {

	public static final ResponseHandler<RhinoResponse> DEFAULT = new ResponseHandler<RhinoResponse>() {
		@Override
		public RhinoResponse onFail(RhinoScript script, Throwable ex, long elapsedTime) {
			ex.printStackTrace() ;
			return RhinoResponse.fail(script, ex, elapsedTime);
		}

		@Override
		public RhinoResponse onSuccess(RhinoScript script, Object rtnValue, long elapsedTime) {
			return RhinoResponse.create(script, rtnValue, elapsedTime);
		}
	};

	public static final ResponseHandler<Boolean> FalseOnError = new ResponseHandler<Boolean>() {
		@Override
		public Boolean onFail(RhinoScript script, Throwable ex, long elapsedTime) {
			return Boolean.FALSE;
		}
		@Override
		public Boolean onSuccess(RhinoScript script, Object rtnValue, long elapsedTime) {
			return Boolean.TRUE;
		}
	};
	
	public static final ResponseHandler<String> StringMessage = new ResponseHandler<String>() {
		@Override
		public String onFail(RhinoScript script, Throwable ex, long elapsedTime) {
			return ex.getMessage();
		}

		@Override
		public String onSuccess(RhinoScript script, Object rtnValue, long elapsedTime) {
			return (String)rtnValue;
		}
	} ;

	public abstract T onSuccess(RhinoScript script, Object rtnValue, long elapsedTime) ;
	public abstract T onFail(RhinoScript script, Throwable ex, long elapsedTime) ;
}
