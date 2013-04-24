package net.ion.craken.node;



public interface WriteSession {

	public WriteNode pathBy(String fqn)  ;

	public WriteNode root()  ;

	public boolean exists(String fqn) ;

	public void failRollback();

	public void endCommit();
	
	public Credential credential() ;
	
	
}
