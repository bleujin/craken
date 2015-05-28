package net.ion.craken.node.crud.util;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.convert.Functions;
import net.ion.craken.node.crud.ReadChildrenEach;
import net.ion.craken.node.crud.ReadChildrenIterator;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;

public class ReadChildrenEachs {

	public static final ReadChildrenEach<List<ReadNode>> LIST = new ReadChildrenEach<List<ReadNode>>() {
		@Override
		public List<ReadNode> handle(ReadChildrenIterator citer) {
			List<ReadNode> result = ListUtil.newList() ;
			while(citer.hasNext()){
				result.add(citer.next()) ;
			}
			return result;
		}
	};
	public static final ReadChildrenEach<Void> DEBUG = new ReadChildrenEach<Void>(){
		@Override
		public Void handle(ReadChildrenIterator citer) {
			while(citer.hasNext()){
				ReadNode next = citer.next();
				citer.session().credential().tracer().println(next.fqn() + ","  + next.transformer(Functions.READ_TOFLATMAP)) ;
			}
			return null;
		}
	};
	public static final ReadChildrenEach<Set<String>> CHILDREN_NAME = new ReadChildrenEach<Set<String>>(){
		@Override
		public Set<String> handle(ReadChildrenIterator citer) {
			Set result = SetUtil.newSet() ;
			while(citer.hasNext()){
				result.add(citer.next().fqn().name()) ;
			}
			return result;
		}
	};
	
	public static final ReadChildrenEach<Integer> COUNT = new ReadChildrenEach<Integer>(){
		@Override
		public Integer handle(ReadChildrenIterator citer) {
			return citer.count();
		}
	};

	public static final ReadChildrenEach<ReadNode> FIRSTNODE = new ReadChildrenEach<ReadNode>() {
		@Override
		public ReadNode handle(ReadChildrenIterator citer) {
			return citer.hasNext() ? citer.next() : null;
		}
	};
	public static ReadChildrenEach<IteratorList<ReadNode>> ITERATOR = new ReadChildrenEach<IteratorList<ReadNode>>() {

		@Override
		public IteratorList<ReadNode> handle(final ReadChildrenIterator citer) {
			return new IteratorList<ReadNode>() {
				
				@Override
				public Iterator<ReadNode> iterator() {
					return citer;
				}
				
				@Override
				public ReadNode next() {
					return citer.next();
				}
				
				@Override
				public boolean hasNext() {
					return citer.hasNext();
				}
				
				@Override
				public List<ReadNode> toList() {
					return citer.list() ;
				}
				
				public int count(){
					return citer.count() ;
				}
			};
		}
	};
}
