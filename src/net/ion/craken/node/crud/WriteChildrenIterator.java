package net.ion.craken.node.crud;

import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;

public class WriteChildrenIterator implements Iterable<WriteNode>, Iterator<WriteNode> {

	private WriteSession session;
	private List<WriteNode> list;
	private int index = 0 ;

	WriteChildrenIterator(WriteSession session, List<WriteNode> list) {
		this.session = session ;
		this.list = list ;
	}
	
	final static WriteChildrenIterator create(WriteSession session, List<WriteNode> list){
		return new WriteChildrenIterator(session, list) ;
	}
	
	public List<WriteNode> list(){
		return list ;
	}
	
	@Override
	public Iterator<WriteNode> iterator() {
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
	public WriteNode next() {
		return list.get(index++);
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("readonly");
	}

	public WriteSession session() {
		return session;
	}
}
