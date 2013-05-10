package net.ion.craken.node.crud;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;
import com.google.common.base.Predicate;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
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

public class ReadChildren extends IteratorList<ReadNode>{

	private ReloadIterator iter ;

	
	private boolean needReload = false;
	private int skip = 0 ;
	private int offset = 100;
	private List<SortElement> sorts = ListUtil.newList() ;
	private Predicate<ReadNode> filter;
	
	private final ReadSession session ;
	public ReadChildren(ReadSession session, Iterator<TreeNode<PropertyId, PropertyValue>> iter){
		this.session = session ;
		this.iter = new ReloadIterator(session, iter);
	}
	
	@Override
	public boolean hasNext() {
		checkReload() ;
		return iter.hasNext();
	}

	@Override
	public ReadNode next() {
		checkReload() ;
		return iter.next() ;
	}

	private void checkReload() {
		if (needReload){
			this.iter = iter.reload(this.skip, this.offset, this.filter, this.sorts) ;
			this.needReload = false ;
		}
		
	}

	public ReadChildren skip(int skip){
		this.skip = skip ;
		this.needReload = true ;
		return this ;
	}
	
	public ReadChildren offset(int offset){
		this.offset = offset ;
		this.needReload = true ;
		return this ;
	}
	
	public ReadChildren ascending(String propId){
		sorts.add(new SortElement(propId, true)) ;
		this.needReload = true ;
		return this ;
	}

	public ReadChildren descending(String propId){
		sorts.add(new SortElement(propId, false)) ;
		this.needReload = true ;
		return this ;
	}
	
	public ReadChildren filter(Predicate<ReadNode> filter){
		this.filter = filter ;
		this.needReload = true ;
		return this ;
	}

	
	public Iterator<ReadNode> iterator(){
		return toList().iterator() ;
	}
	
	public List<ReadNode> toList(){
		List<ReadNode> result = ListUtil.newList() ;
		while(hasNext()){
			result.add(next()) ;
		}
		return result ;
	}
	
	public <T> T transform(Function<Iterator<ReadNode>, T> fn){
		return fn.apply(iterator()) ;
	}

	public Rows toRows(String... cols) throws SQLException{
		ColumnParser cparser = session.getWorkspace().getAttribute(ColumnParser.class.getCanonicalName(), ColumnParser.class);
		return CrakenNodeRows.create(session, iterator(), cparser.parse(cols));
	}

	public void debugPrint() {
		while(hasNext()){
			Debug.debug(next()) ;
		}
	}

	public Rows toRows(Page page, String... cols) throws SQLException {
		skip(page.getSkipOnScreen()).offset(page.getOffsetOnScreen()) ;
		
		ColumnParser cparser = session.getWorkspace().getAttribute(ColumnParser.class.getCanonicalName(), ColumnParser.class);
		final List<ReadNode> screenList = toList();
		int count = screenList.size() ;
		return CrakenNodeRows.create(session, page.subList(screenList).iterator(), cparser.parse(cols).append(new ConstantColumn(count, "cnt")));
	}
}


class ReloadIterator implements Iterator<ReadNode>{

	private ReadSession session ;
	private Iterator<TreeNode<PropertyId, PropertyValue>> oriIter ;
	
	public ReloadIterator(ReadSession session, Iterator<TreeNode<PropertyId, PropertyValue>> iter) {
		this.session = session ;
		this.oriIter = iter ;
	}
	
	public ReloadIterator reload(int skip, int offset, final Predicate<ReadNode> filter, final List<SortElement> sorts) {
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
				if (filter == null) return true ;
				return filter.apply(ReadNodeImpl.load(session, treeNode));
			}
		};
		
		List<TreeNode<PropertyId, PropertyValue>> result = SortUtil.selectTopN(oriIter, modFilter, comparator, skip + offset);
		return new ReloadIterator(session, result.subList(skip, result.size()).iterator()) ;
	}

	@Override
	public boolean hasNext() {
		return oriIter.hasNext();
	}

	@Override
	public ReadNode next() {
		return ReadNodeImpl.load(session, oriIter.next());
	}

	@Override
	public void remove() {
		
	}
	
}


class SortElement {
	String propId ;
	boolean ascending ;
	
	public SortElement(String propId, boolean ascending) {
		this.propId = propId ;
		this.ascending = ascending ;
	}
}
