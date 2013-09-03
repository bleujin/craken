package net.ion.craken.node;

import java.util.List;
import java.util.Set;

import net.ion.craken.tree.Fqn;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;
import net.ion.nsearcher.config.Central;
import net.ion.nsearcher.index.IndexJob;
import net.ion.nsearcher.index.IndexSession;
import net.ion.nsearcher.index.Indexer;

public class DumpSession {

	private ReadSession readSession;
	private Workspace workspace;
	private Central central;
	private Indexer indexer;
	private IndexWriteConfig iwconfig = new IndexWriteConfig() ;
	private Set<Fqn> ancestors = SetUtil.newSet() ;
	
	private List<DumpNode> list ;
	public DumpSession(ReadSession readSession, Workspace workspace) {
		this.readSession = readSession ;
		this.workspace = workspace ;
		this.central = workspace.central();
	}
	
	public int endCommit() {
		for (Fqn parent : ancestors) {
			workspace.pathNode(this.iwconfig, parent) ;
		}
		
		return indexer.index(new IndexJob<Integer>() {
			@Override
			public Integer handle(IndexSession isession) throws Exception {
				
				isession.setIgnoreBody(iwconfig.isIgnoreBodyField()) ;
				
				int count = 0 ;
				for (DumpNode dnode : list) {
					dnode.apply(isession) ;
					count++;
				}
				return count;
			}
		}) ;
	}

	public void failRollback() {
		
	}

	public DumpNode createBy(String fqn) {
		final Fqn self = forCreateAncestor(fqn);
		final DumpNode result = DumpNode.insert(this, this.indexer, self);
		list.add(result) ;
		return result ;
	}

	public DumpNode resetBy(String fqn) {
		final Fqn self = forCreateAncestor(fqn);
		final DumpNode result = DumpNode.update(this, this.indexer, self);
		list.add(result) ;
		return result ;
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
	
	public Credential credential() {
		return readSession.credential();
	}
	
	public Workspace workspace() {
		return workspace;
	}
	
	public ReadSession readSession(){
		return readSession ;
	}
	
	public void continueUnit(){
		endCommit() ;
		beginTran() ;
	}

	public void beginTran() {
		this.indexer = central.newIndexer() ;
		this.list = ListUtil.newList() ;
	}

	public IndexWriteConfig indexConfig() {
		return iwconfig ;
	}
}
