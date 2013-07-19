package net.ion.craken.node;

import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.StringUtil;

public abstract class AbstractWriteSession implements WriteSession{

	private ReadSession readSession ;
	private Workspace workspace ;
	protected AbstractWriteSession(ReadSession readSession, Workspace workspace){
		this.readSession = readSession ;
		this.workspace = workspace ;
	}
	
	public WriteNode resetBy(String fqn){
		return WriteNodeImpl.loadTo(this, workspace.resetNode(fqn)) ;
	}
	
	public WriteNode pathBy(String fqn) {
		return WriteNodeImpl.loadTo(this, workspace.getNode(fqn)) ;
	}

	public WriteNode pathBy(String fqn0, String... fqns) {
		return WriteNodeImpl.loadTo(this, workspace.getNode(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/')))) ;
	}

	public WriteNode pathBy(Fqn fqn) {
		return WriteNodeImpl.loadTo(this, workspace.getNode(fqn)) ;
	}
	
	public WriteNode root() {
		return pathBy("/");
	}

	public boolean exists(String fqn) {
		return workspace.exists(fqn) ;
	}

	@Override
	public Credential credential() {
		return readSession.credential();
	}

	@Override
	public Workspace workspace() {
		return workspace;
	}
	
	@Override
	public void endCommit() {
		
	}
	
	@Override
	public void failRollback() {
		
	}
	
	protected ReadSession readSession(){
		return readSession ;
	}

	@Override
	public void notifyTouch(Fqn fqn, Touch touch) {
		
	}
	
	public void continueUnit(){
		workspace().continueUnit(this) ;
	}
}
