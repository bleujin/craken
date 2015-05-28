package net.ion.craken.node.crud;

import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;

public class ReadChildrenIterator implements Iterable<ReadNode>, Iterator<ReadNode> {

	private ReadSession session;
	private List<ReadNode> list;
	private int index = 0 ;

	ReadChildrenIterator(ReadSession session, List<ReadNode> list) {
		this.session = session ;
		this.list = list ;
	}
	
	final static ReadChildrenIterator create(ReadSession session, List<ReadNode> list){
		return new ReadChildrenIterator(session, list) ;
	}
	
	public List<ReadNode> list(){
		return list ;
	}
	
	@Override
	public Iterator<ReadNode> iterator() {
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
	public ReadNode next() {
		return list.get(index++);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("readonly");
	}

	public ReadSession session() {
		return session;
	}
}
