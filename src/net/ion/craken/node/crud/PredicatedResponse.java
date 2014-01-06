package net.ion.craken.node.crud;

import java.io.IOException;
import java.util.List;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.util.ResponsePredicate;
import net.ion.craken.tree.Fqn;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

public class PredicatedResponse {

	private final ResponsePredicate predicate;
	private final ReadSession session;
	private final List<Fqn> founded;

	private PredicatedResponse(ResponsePredicate predicate, ReadSession session, List<Fqn> result) {
		this.predicate = predicate ;
		this.session = session ;
		this.founded = result ;
	}

	public static PredicatedResponse create(ResponsePredicate predicate, ReadSession session, List<Fqn> result) {
		return new PredicatedResponse(predicate, session, result);
	}
	
	public ResponsePredicate predicate(){
		return predicate ;
	}
	
	public int size(){
		return founded.size() ;
	}

	public PredicatedResponse predicated(ResponsePredicate predicate) {
		List<Fqn> result = ListUtil.newList() ;
		for (Fqn fqn : founded) {
			if (predicate.apply(session, fqn)) result.add(fqn) ;
		} 
		return PredicatedResponse.create(predicate, session, result);
	}

	public ReadNode first() {
		return founded.size() == 0 ? null : toList().get(0);
	}
	
	public List<ReadNode> toList(){
		List<ReadNode> list = ListUtil.newList() ;
		for (Fqn fqn : founded) {
			list.add(session.pathBy(fqn)) ;
		}
		return list ;
	}

	public void debugPrint() throws IOException {
		for (ReadNode node : toList()) {
			Debug.line(node) ;
		}
	}

	public ReadNode readNode(int index) {
		final Fqn fqn = founded.get(index);
		return session.pathBy(fqn) ;
	}

	public NodeCommon<ReadNode> last() {
		return readNode(founded.size() -1);
	}

	public int totoalCount() {
		return founded.size() ;
	}
}
