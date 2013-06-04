package net.ion.craken.node;




public interface WriteSession extends ISession<WriteNode> {

	public WriteNode pathBy(String fqn0, String... fqns)  ;

	public void failRollback();

	public void endCommit();
	
	public Credential credential() ;

	public Workspace workspace() ;
}
