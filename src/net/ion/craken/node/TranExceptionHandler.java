package net.ion.craken.node;


public interface TranExceptionHandler {

	public final static TranExceptionHandler PRINT = new TranExceptionHandler(){
		@Override
		public void handle(WriteSession tsession, TransactionJob tjob, Throwable ex) {
			ex.printStackTrace() ;
		}

	} ;
		
	public void handle(WriteSession tsession, TransactionJob tjob, Throwable ex) ;

}
