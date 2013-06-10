package net.ion.craken.node;


public interface TranExceptionHandler {

	public TranExceptionHandler PRINT = new TranExceptionHandler(){
		@Override
		public void handle(WriteSession tsession, Throwable ex) {
			ex.printStackTrace() ;
		}} ;
		
	public TranExceptionHandler NULL = null;
	
	public void handle(WriteSession tsession, Throwable ex) ;

}
