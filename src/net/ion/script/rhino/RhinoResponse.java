package net.ion.script.rhino;


public abstract class RhinoResponse {

	private RhinoScript script;
	private Object rtnValue;
	private long elapsedTime;
	private boolean isOk;
	
	public RhinoResponse(RhinoScript script, Object rtnValue, long elapsedTime, boolean isOk) {
		this.script = script ;
		this.rtnValue = rtnValue ;
		this.elapsedTime = elapsedTime ;
		this.isOk = isOk ;
	}

	public static RhinoResponse create(RhinoScript script, Object rtnValue, long elapsedTime) {
		return new SuccessResponse(script, rtnValue, elapsedTime);
	}

	public static RhinoResponse fail(RhinoScript script, Throwable ex, long elapsedTime) {
		return new FailResponse(script, ex, elapsedTime);
	}

	public boolean isOk() {
		return isOk ;
	}
	
	public <T> T getReturn(Class<T> clz) {
		return (T)rtnValue;
	}

	public long elapsedTime() {
		return elapsedTime;
	}

	public String script() {
		return script.scriptCode();
	}

}

class SuccessResponse extends RhinoResponse {
	public SuccessResponse(RhinoScript script, Object rtnValue, long elapsedTime) {
		super(script, rtnValue, elapsedTime, true) ;
	}
}

class FailResponse extends RhinoResponse {

	private Exception ex ;
	public FailResponse(RhinoScript script, Throwable ex, long elapsedTime) {
		super(script, null, elapsedTime, false) ;
	}

	public Throwable exception(){
		return ex ;
	}	
}
