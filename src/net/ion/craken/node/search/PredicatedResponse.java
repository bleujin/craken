package net.ion.craken.node.search;

import java.io.IOException;
import java.util.List;

import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.search.util.PredicateArgument;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

import com.google.common.base.Predicate;

public class PredicatedResponse {

	private final Predicate<PredicateArgument> predicate;
	private final List<PredicateArgument> founded;

	private PredicatedResponse(Predicate<PredicateArgument> predicate, List<PredicateArgument> result) {
		this.predicate = predicate ;
		this.founded = result ;
	}

	public static PredicatedResponse create(Predicate<PredicateArgument> predicate, List<PredicateArgument> result) {
		return new PredicatedResponse(predicate, result);
	}
	
	public Predicate<PredicateArgument> predicate(){
		return predicate ;
	}
	
	public int size(){
		return founded.size() ;
	}

	public PredicatedResponse predicated(Predicate<PredicateArgument> predicate) {
		List<PredicateArgument> result = ListUtil.newList() ;
		for (PredicateArgument arg : founded) {
			if (predicate.apply(arg)) result.add(arg) ;
		} 
		return PredicatedResponse.create(predicate, result);
	}

	public ReadNode first() {
		return founded.size() == 0 ? null : toList().get(0);
	}
	
	public List<ReadNode> toList(){
		List<ReadNode> list = ListUtil.newList() ;
		for (PredicateArgument arg : founded) {
			list.add(arg.session().pathBy(arg.fqn())) ;
		}
		return list ;
	}

	public void debugPrint() throws IOException {
		for (ReadNode node : toList()) {
			Debug.line(node) ;
		}
	}

	public ReadNode readNode(int index) {
		final PredicateArgument arg = founded.get(index);
		return arg.session().pathBy(arg.fqn()) ;
	}

	public NodeCommon<ReadNode> last() {
		return readNode(founded.size() -1);
	}
}
