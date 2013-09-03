package net.ion.craken.node;

import java.io.IOException;
import java.util.Set;

import net.ion.craken.node.crud.ChildQueryRequest;
import net.ion.craken.node.crud.WriteNodeImpl;
import net.ion.craken.node.crud.WriteNodeImpl.Touch;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.SetUtil;
import net.ion.framework.util.StringUtil;

import org.apache.lucene.queryparser.classic.ParseException;

public abstract class AbstractWriteSession implements WriteSession{

	private ReadSession readSession ;
	private Workspace workspace ;
	private Set<String> ignoreProperyIds = SetUtil.newSet();
	private IndexWriteConfig iwconfig = new IndexWriteConfig() ;
	
	protected AbstractWriteSession(ReadSession readSession, Workspace workspace){
		this.readSession = readSession ;
		this.workspace = workspace ;
	}
	
	private Set<Fqn> ancestors = SetUtil.newSet() ;
	
	public WriteNode createBy(String fqn){
		final Fqn self = forCreateAncestor(fqn);
		return WriteNodeImpl.loadTo(this, workspace.createNode(iwconfig, self)) ;
	}
	
	public WriteNode resetBy(String fqn){
		final Fqn self = forCreateAncestor(fqn);
		return WriteNodeImpl.loadTo(this, workspace.resetNode(iwconfig, self)) ;
	}

	private Fqn forCreateAncestor(String fqn) {
		final Fqn self = Fqn.fromString(fqn);
		Fqn parent = self.getParent() ;
		while(! parent.isRoot()) {
			ancestors.add(parent) ;
			parent = parent.getParent() ;
		}
		return self;
	}
	
	public WriteNode pathBy(String fqn) {
		return pathBy(Fqn.fromString(fqn)) ;
	}

	public WriteNode pathBy(String fqn0, String... fqns) {
		return pathBy(Fqn.fromString((fqn0.startsWith("/") ? fqn0 : "/" + fqn0) + '/' + StringUtil.join(fqns, '/'))) ;
	}

	public WriteNode pathBy(Fqn fqn) {
		return WriteNodeImpl.loadTo(this, workspace.pathNode(this.iwconfig, fqn)) ;
	}
	
	public WriteNode root() {
		return pathBy("/");
	}

	public boolean exists(String fqn) {
		return workspace.exists(Fqn.fromString(fqn)) ;
	}

	
	@Override
	public void endCommit() {
		for (Fqn parent : ancestors) {
			workspace.pathNode(this.iwconfig, parent) ;
		}
	}
	
	@Override
	public void failRollback() {
		
	}

	@Override
	public void notifyTouch(Fqn fqn, Touch touch) {
		
	}
	@Override
	public Credential credential() {
		return readSession.credential();
	}
	
	@Override
	public Workspace workspace() {
		return workspace;
	}
	
	public ReadSession readSession(){
		return readSession ;
	}
	
	public void continueUnit(){
		workspace().continueUnit(this) ;
	}
	
	public IndexWriteConfig fieldIndexConfig() {
		return iwconfig ;
	}
	
//	public WriteSession ignoreIndex(String... fields){
//		for (String field : fields) {
//			ignoreProperyIds.add(field) ;
//		}
//		return this ;
//	}
//	
//	public PropertyId idInfoTo(PropertyId pid){
//		if (ignoreProperyIds.contains(pid.idString())){
//			pid.ignoreIndex() ;
//		}
//		
//		return pid ;
//	}
	
	@Override
	public ChildQueryRequest queryRequest(String query) throws IOException, ParseException {
		return root().childQuery(query, true);
	}
}
