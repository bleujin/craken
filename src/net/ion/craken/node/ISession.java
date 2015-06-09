package net.ion.craken.node;

import net.ion.craken.node.crud.tree.Fqn;

public interface ISession<T extends NodeCommon<T>> {

	public T pathBy(Fqn fqn);

	public T root()  ;

	public boolean exists(String fqn) ;

	public boolean exists(Fqn fqn) ;

	public T pathBy(String fqn);

	public Credential credential();

	public Workspace workspace() ;
}
