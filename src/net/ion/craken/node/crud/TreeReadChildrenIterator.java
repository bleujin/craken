package net.ion.craken.node.crud;

import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.framework.util.ListUtil;

public class TreeReadChildrenIterator implements Iterable<TreeReadNode>, Iterator<TreeReadNode> {

	private ReadSession session;
	private List<TreeReadNode> list;
	private int index = 0 ;

	TreeReadChildrenIterator(ReadSession session, List<TreeReadNode> list) {
		this.session = session ;
		this.list = list ;
	}
	
	final static TreeReadChildrenIterator create(ReadSession session, List<TreeReadNode> list){
		return new TreeReadChildrenIterator(session, list) ;
	}
	
	@Override
	public Iterator<TreeReadNode> iterator() {
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
	public TreeReadNode next() {
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
		List<TreeNode> tnodes = ListUtil.newList() ;
		for (TreeReadNode trn : list) {
			tnodes.add(trn.treeNode()) ;
		}
		return new ReadChildren(session, null, tnodes.iterator());
	}

}
