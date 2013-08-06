package net.ion.craken.node;

import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.craken.tree.PropertyId;




public interface WriteSession extends ISession<WriteNode> {

	public WriteNode resetBy(String fqn);
	
	public WriteNode pathBy(String fqn0, String... fqns)  ;

	public WriteNode createBy(String fqn);

	public void failRollback();

	public void endCommit();
	
	public Credential credential() ;

	public Workspace workspace() ;

	public void notifyTouch(Fqn fqn, Touch touch);

	public void continueUnit();

	public WriteSession ignoreIndex(String... fields);

	public PropertyId idInfoTo(PropertyId pid) ;

}
