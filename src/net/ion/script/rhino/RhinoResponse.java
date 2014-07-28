package net.ion.script.rhino;

public interface RhinoResponse<T> {
	public final static RhinoResponse<Object> ReturnNative = new RhinoResponse<Object>() {
		@Override
		public Object onSuccess(String fullName, Object[] args, Object result) {
			return result;
		}

		@Override
		public Object onThrow(String fullName, Object[] args, Exception ex) {
			ex.printStackTrace();
			return ex;
		}
	};

	public T onSuccess(String fullName, Object[] args, Object result);

	public T onThrow(String fullName, Object[] args, Exception ex);
}
