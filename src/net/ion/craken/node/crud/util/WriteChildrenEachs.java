package net.ion.craken.node.crud.util ;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.WriteChildrenEach;
import net.ion.craken.node.crud.WriteChildrenIterator;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;

public class WriteChildrenEachs {

	public static final WriteChildrenEach<List<WriteNode>> LIST = new WriteChildrenEach<List<WriteNode>>() {
		@Override
		public List<WriteNode> handle(WriteChildrenIterator citer) {
			List<WriteNode> result = ListUtil.newList() ;
			while(citer.hasNext()){
				result.add(citer.next()) ;
			}
			return result;
		}
	};
	public static final WriteChildrenEach<Void> DEBUG = new WriteChildrenEach<Void>(){
		@Override
		public Void handle(WriteChildrenIterator citer) {
			while(citer.hasNext()){
				WriteNode next = citer.next();
				citer.session().credential().tracer().println(next.fqn() + "," + next.transformer(Functions.WRITE_TOFLATMAP)) ;
			}
			return null;
		}
	};
	
	public static final WriteChildrenEach<Set<String>> CHILDREN_NAME = new WriteChildrenEach<Set<String>>(){
		@Override
		public Set<String> handle(WriteChildrenIterator citer) {
			Set result = SetUtil.newSet() ;
			while(citer.hasNext()){
				result.add(citer.next().fqn().name()) ;
			}
			return result;
		}
	};
	
	public static final WriteChildrenEach<Integer> COUNT = new WriteChildrenEach<Integer>(){
		@Override
		public Integer handle(WriteChildrenIterator citer) {
			return citer.count();
		}
	};
	public static final WriteChildrenEach<IteratorList<WriteNode>> ITERATOR = new WriteChildrenEach<IteratorList<WriteNode>>() {

		@Override
		public IteratorList<WriteNode> handle(final WriteChildrenIterator citer) {
			return new IteratorList<WriteNode>() {
				
				@Override
				public Iterator<WriteNode> iterator() {
					return citer;
				}
				
				@Override
				public WriteNode next() {
					return citer.next();
				}
				
				@Override
				public boolean hasNext() {
					return citer.hasNext();
				}
				
				@Override
				public List<WriteNode> toList() {
					return citer.list();
				}
				
				public int count(){
					return citer.count() ;
				}
			};
		}
	};
	public static final WriteChildrenEach<WriteNode> FIRSTNODE = new WriteChildrenEach<WriteNode>() {
		@Override
		public WriteNode handle(WriteChildrenIterator citer) {
			return citer.hasNext() ? citer.next() : null;
		}
	};
}
