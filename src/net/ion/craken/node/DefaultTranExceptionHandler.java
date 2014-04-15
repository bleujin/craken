package net.ion.craken.node;

public class DefaultTranExceptionHandler implements TranExceptionHandler {

	@Override
	public void handle(WriteSession tsession, TransactionJob tjob, Throwable ex) {
		ex.printStackTrace(); 
	}

}
