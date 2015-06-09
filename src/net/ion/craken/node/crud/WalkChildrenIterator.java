package net.ion.craken.node.crud;

import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.ReadSession;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.framework.util.ListUtil;

public class WalkChildrenIterator implements Iterable<WalkReadNode>, Iterator<WalkReadNode> {

	private ReadSession session;
	private List<WalkReadNode> list;
	private int index = 0 ;

	WalkChildrenIterator(ReadSession session, List<WalkReadNode> list) {
		this.session = session ;
		this.list = list ;
	}
	
	final static WalkChildrenIterator create(ReadSession session, List<WalkReadNode> list){
		return new WalkChildrenIterator(session, list) ;
	}
	
	@Override
	public Iterator<WalkReadNode> iterator() {
		return this;
	}

	@Override
	public boolean hasNext() {
		return list.size() > index;
	}

	public int count() {
		return list.size() ;
	}

	@Override
	public WalkReadNode next() {
		return list.get(index++);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("readonly");
	}

	public ReadSession session() {
		return session;
	}

	public ReadChildren toReadChildren() {
		List<Fqn> tnodes = ListUtil.newList() ;
		for (WalkReadNode trn : list) {
			tnodes.add(trn.fqn()) ;
		}
		return new ReadChildren(session, null, tnodes.iterator());
	}

}
