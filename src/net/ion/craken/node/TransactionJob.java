package net.ion.craken.node;

public interface TransactionJob<T> {

	public final static TransactionJob<Void> BLANK = new TransactionJob<Void>() {
		@Override
		public Void handle(WriteSession wsession) throws Exception {
			return null;
		}
	};
	
	public T handle(WriteSession wsession) throws Exception ;
}
