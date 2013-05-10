package net.ion.craken.node;

import net.ion.craken.tree.Fqn;



public interface WriteSession {

	public WriteNode pathBy(String fqn0, String... fqns)  ;

	public WriteNode pathBy(Fqn fqn);

	public WriteNode root()  ;

	public boolean exists(String fqn) ;

	public void failRollback();

	public void endCommit();
	
	public Credential credential() ;

	
}
