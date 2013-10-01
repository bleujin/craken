package net.ion.craken.node.crud;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.ion.craken.io.GridFilesystem;
import net.ion.craken.node.AbstractChildren;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.SortElement;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.util.SortUtil;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.util.ListUtil;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

public class WriteChildren  extends AbstractChildren<WriteNode, WriteChildren> implements Iterable<WriteNode> {

	private ReloadWriteIterator iter ;

	
	private boolean needReload = false;
	private int skip = 0 ;
	private int offset = 1000;
	private List<SortElement> sorts = ListUtil.newList() ;
	private List<Predicate<WriteNode>> filters;
	
	private final WriteSession session ;
	public WriteChildren(WriteSession session, Iterator<TreeNode> iter){
		this.session = session ;
		this.iter = new ReloadWriteIterator(session, iter);
	}
	
	@Override
	public boolean hasNext() {
		checkReload() ;
		return iter.hasNext();
	}

	@Override
	public WriteNode next() {
		checkReload() ;
		return iter.next() ;
	}

	private void checkReload() {
		if (needReload){
			this.iter = iter.reload(this.skip, this.offset, this.filters, this.sorts) ;
			this.needReload = false ;
		}
		
	}

	public WriteChildren skip(int skip){
		this.skip = skip ;
		this.needReload = true ;
		return this ;
	}
	
	public WriteChildren offset(int offset){
		this.offset = offset ;
		this.needReload = true ;
		return this ;
	}
	
	public WriteChildren ascending(String propId){
		sorts.add(new SortElement(propId, true)) ;
		this.needReload = true ;
		return this ;
	}

	public WriteChildren descending(String propId){
		sorts.add(new SortElement(propId, false)) ;
		this.needReload = true ;
		return this ;
	}
	
	public WriteChildren filter(Predicate<WriteNode> filter){
		filters.add(filter) ;
		this.needReload = true ;
		return this ;
	}
	
	


}


class ReloadWriteIterator implements Iterator<WriteNode>{

	private WriteSession session ;
	private Iterator<TreeNode> oriIter ;
	
	public ReloadWriteIterator(WriteSession session, Iterator<TreeNode> iter) {
		this.session = session ;
		this.oriIter = iter ;
	}
	
	private GridFilesystem gfs(){
		return session.workspace().gfs() ;
	}
	
	public ReloadWriteIterator reload(int skip, int offset, final List<Predicate<WriteNode>> filters, final List<SortElement> sorts) {
		Comparator<TreeNode> mycomparator = new Comparator<TreeNode>(){
			@Override
			public int compare(TreeNode left, TreeNode right) {
				
				for (SortElement sele : sorts) {
					PropertyValue leftProperty = left.get(gfs(), PropertyId.normal(sele.propid()));
					PropertyValue rightProperty = right.get(gfs(), PropertyId.normal(sele.propid())) ;
					
					if (leftProperty == null || rightProperty == null ) return 0 ;
					
					return leftProperty.compareTo(rightProperty) * (sele.ascending() ? -1 : 1);
				}
				
				return 0;
			}
		} ;
		
		Predicate<TreeNode> myfilter = new Predicate<TreeNode>() {
			@Override
			public boolean apply(TreeNode treeNode) {
				if (filters.size() == 0) return true ;
				for (Predicate<WriteNode> filter : filters) {
					if (! filter.apply(WriteNodeImpl.loadTo(session, treeNode))) return false;
				}
				return true;
			}
		};
		
		if (sorts.size() == 0) { // no sort
			Iterator<TreeNode> iterator = Iterators.limit(Iterators.filter(oriIter, myfilter), skip + offset);
			Iterators.advance(iterator, skip) ;
			return new ReloadWriteIterator(session, iterator);
		} 
		
		List<TreeNode> result = SortUtil.selectTopN(oriIter, myfilter, mycomparator, skip + offset);
		return new ReloadWriteIterator(session, result.subList(skip, result.size()).iterator()) ;
	}

	@Override
	public boolean hasNext() {
		return oriIter.hasNext();
	}

	@Override
	public WriteNode next() {
		return WriteNodeImpl.loadTo(session, oriIter.next());
	}

	@Override
	public void remove() {
		
	}
	
}

