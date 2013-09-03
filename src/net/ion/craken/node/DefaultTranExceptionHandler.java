package net.ion.craken.node;

public class DefaultTranExceptionHandler implements TranExceptionHandler {

	@Override
	public void handle(WriteSession tsession, Throwable ex) {
	}

	@Override
	public void handle(DumpSession dsession, Throwable ex) {
	}

}
