package net.ion.craken.node.search.util;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.tree.Fqn;

public class PredicateArgument {

	private ReadSession session;
	private Fqn fqn;

	public PredicateArgument(ReadSession session, Fqn fqn) {
		this.session = session ;
		this.fqn = fqn ;
	}

	public final static PredicateArgument create(ReadSession session, Fqn fqn){
		return new PredicateArgument(session, fqn) ;
	} 
	
	public ReadSession session(){
		return session ;
	}
	
	public Fqn fqn(){
		return fqn ;
	}
	
	public ReadNode node(){
		return session.pathBy(fqn) ;
	}
}
