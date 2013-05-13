package net.ion.craken.node.crud;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.convert.Predicates;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.ConstantColumn;
import net.ion.craken.node.convert.rows.CrakenNodeRows;
import net.ion.craken.node.search.util.SortUtil;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.db.Page;
import net.ion.framework.db.Rows;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

public class WriteChildren  extends IteratorList<WriteNode>{

	private ReloadWriteIterator iter ;

	
	private boolean needReload = false;
	private int skip = 0 ;
	private int offset = 100;
	private List<SortElement> sorts = ListUtil.newList() ;
	private List<Predicate<WriteNode>> filters;
	
	private final WriteSession session ;
	public WriteChildren(WriteSession session, Iterator<TreeNode<PropertyId, PropertyValue>> iter){
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

	public WriteChildren eq(String propId, Object value) {
		return filter(Predicates.<WriteNode>propertyEqual(propId, value));
	}

	public WriteChildren contains(String propId, Object value) {
		return filter(Predicates.<WriteNode>propertyHasValue(propId, value));
	}

	
	public Iterator<WriteNode> iterator(){
		return toList().iterator() ;
	}
	
	public List<WriteNode> toList(){
		List<WriteNode> result = ListUtil.newList() ;
		while(hasNext()){
			result.add(next()) ;
		}
		return result ;
	}
	
	public <T> T transform(Function<Iterator<WriteNode>, T> fn){
		return fn.apply(iterator()) ;
	}

	public void debugPrint() {
		while(hasNext()){
			Debug.debug(next()) ;
		}
	}



}


class ReloadWriteIterator implements Iterator<WriteNode>{

	private WriteSession session ;
	private Iterator<TreeNode<PropertyId, PropertyValue>> oriIter ;
	
	public ReloadWriteIterator(WriteSession session, Iterator<TreeNode<PropertyId, PropertyValue>> iter) {
		this.session = session ;
		this.oriIter = iter ;
	}
	
	public ReloadWriteIterator reload(int skip, int offset, final List<Predicate<WriteNode>> filters, final List<SortElement> sorts) {
		Comparator<TreeNode<PropertyId, PropertyValue>> comparator = new Comparator<TreeNode<PropertyId, PropertyValue>>(){
			@Override
			public int compare(TreeNode<PropertyId, PropertyValue> left, TreeNode<PropertyId, PropertyValue> right) {
				
				for (SortElement sele : sorts) {
					PropertyValue leftProperty = left.get(PropertyId.normal(sele.propId));
					PropertyValue rightProperty = right.get(PropertyId.normal(sele.propId)) ;
					
					if (leftProperty == null || rightProperty == null ) return 0 ;
					
					return leftProperty.compareTo(rightProperty) * (sele.ascending ? -1 : 1);
				}
				
				return 0;
			}
		} ;
		
		Predicate<TreeNode<PropertyId, PropertyValue>> modFilter = new Predicate<TreeNode<PropertyId, PropertyValue>>() {
			@Override
			public boolean apply(TreeNode<PropertyId, PropertyValue> treeNode) {
				if (filters.size() == 0) return true ;
				for (Predicate<WriteNode> filter : filters) {
					if (! filter.apply(WriteNodeImpl.loadTo(session, treeNode))) return false;
				}
				return true;
			}
		};
		
		List<TreeNode<PropertyId, PropertyValue>> result = SortUtil.selectTopN(oriIter, modFilter, comparator, skip + offset);
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

