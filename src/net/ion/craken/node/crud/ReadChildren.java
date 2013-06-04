package net.ion.craken.node.crud;

import java.sql.SQLException;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.SelectProjection;
import net.ion.craken.expression.TerminalParser;
import net.ion.craken.node.AbstractChildren;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.SortElement;
import net.ion.craken.node.convert.rows.AdNodeRows;
import net.ion.craken.node.convert.rows.ColumnParser;
import net.ion.craken.node.convert.rows.ConstantColumn;
import net.ion.craken.node.convert.rows.CrakenNodeRows;
import net.ion.craken.node.search.util.SortUtil;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNode;
import net.ion.framework.db.Page;
import net.ion.framework.db.Rows;
import net.ion.framework.util.ListUtil;
import net.ion.rosetta.Parser;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

public class ReadChildren extends AbstractChildren<ReadNode, ReadChildren> {

	private ReloadIterator iter;

	private boolean needReload = false;
	private int skip = 0;
	private int offset = 1000;
	private List<SortElement> sorts = ListUtil.newList();
	private List<Predicate<ReadNode>> filters = ListUtil.newList();

	private final ReadSession session;

	public ReadChildren(ReadSession session, Iterator<TreeNode<PropertyId, PropertyValue>> iter) {
		this.session = session;
		this.iter = new ReloadIterator(session, iter);
	}

	@Override
	public boolean hasNext() {
		checkReload();
		return iter.hasNext();
	}

	@Override
	public ReadNode next() {
		checkReload();
		return iter.next();
	}

	private void checkReload() {
		if (needReload) {
			this.iter = iter.reload(this.skip, this.offset, this.filters, this.sorts);
			this.needReload = false;
		}
	}

	public ReadChildren skip(int skip) {
		this.skip = skip;
		this.needReload = true;
		return this;
	}

	public ReadChildren offset(int offset) {
		this.offset = offset;
		this.needReload = true;
		return this;
	}

	public ReadChildren ascending(String propId) {
		sorts.add(new SortElement(propId, true));
		this.needReload = true;
		return this;
	}

	public ReadChildren descending(String propId) {
		sorts.add(new SortElement(propId, false));
		this.needReload = true;
		return this;
	}

	public ReadChildren filter(Predicate<ReadNode> filter) {
		filters.add(filter);
		this.needReload = true;
		return this;
	}


	@Deprecated
	public Rows toRows(String... cols) throws SQLException {
		ColumnParser cparser = session.workspace().getAttribute(ColumnParser.class.getCanonicalName(), ColumnParser.class);
		return CrakenNodeRows.create(session, iterator(), cparser.parse(cols));
	}

	@Deprecated
	public Rows toRows(Page _page, String... cols) throws SQLException {
		Page page = (_page == Page.ALL) ? Page.create(10000, 1) : _page; // limit

		checkReload() ;
		Iterators.skip(this.iter, page.getSkipOnScreen()) ;
		Iterator<ReadNode> limitIter = Iterators.limit(this.iter, page.getOffsetOnScreen());
		
		List<ReadNode> screenList = ListUtil.newList() ;
		while(limitIter.hasNext()){
			screenList.add(limitIter.next()) ;
		}

		int count = screenList.size();
		Page pageOnScreen = Page.create(page.getListNum(), page.getPageNo() % page.getScreenCount(), page.getScreenCount()) ;
		
		ColumnParser cparser = session.workspace().getAttribute(ColumnParser.class.getCanonicalName(), ColumnParser.class);
		return CrakenNodeRows.create(session, pageOnScreen.subList(screenList).iterator(), cparser.parse(cols).append(new ConstantColumn(count, "cnt")));
	}

	
	public Rows toAdRows(String expr) {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		SelectProjection sp = TerminalParser.parse(parser, expr);
		return AdNodeRows.create(session, iterator(), sp);
	}


	public Rows toAdRows(Page _page, String expr) {
		Parser<SelectProjection> parser = ExpressionParser.selectProjection();
		SelectProjection sp = TerminalParser.parse(parser, expr);
		Page page = (_page == Page.ALL) ? Page.create(10000, 1) : _page; // limit
		
		checkReload() ;
		Iterators.skip(this.iter, page.getSkipOnScreen()) ;
		Iterator<ReadNode> limitIter = Iterators.limit(this.iter, page.getOffsetOnScreen());
		
		List<ReadNode> screenList = ListUtil.newList() ;
		while(limitIter.hasNext()){
			screenList.add(limitIter.next()) ;
		}

		int count = screenList.size();
		Page pageOnScreen = Page.create(page.getListNum(), page.getPageNo() % page.getScreenCount(), page.getScreenCount()) ;
		return AdNodeRows.create(session,  pageOnScreen.subList(screenList).iterator(), sp, count, "cnt");
	}



}

class ReloadIterator implements Iterator<ReadNode> {

	private ReadSession session;
	private Iterator<TreeNode<PropertyId, PropertyValue>> oriIter;

	public ReloadIterator(ReadSession session, Iterator<TreeNode<PropertyId, PropertyValue>> iter) {
		this.session = session;
		this.oriIter = iter;
	}

	public ReloadIterator reload(int skip, int offset, final List<Predicate<ReadNode>> filters, final List<SortElement> sorts) {

		Comparator<TreeNode<PropertyId, PropertyValue>> mycomparator = new Comparator<TreeNode<PropertyId, PropertyValue>>() {
			@Override
			public int compare(TreeNode<PropertyId, PropertyValue> left, TreeNode<PropertyId, PropertyValue> right) {

				for (SortElement sele : sorts) {
					PropertyValue leftProperty = left.get(PropertyId.normal(sele.propid()));
					PropertyValue rightProperty = right.get(PropertyId.normal(sele.propid()));

					if (leftProperty == null || rightProperty == null)
						return 0;

					return leftProperty.compareTo(rightProperty) * (sele.ascending() ? -1 : 1);
				}

				return 0;
			}
		};

		Predicate<TreeNode<PropertyId, PropertyValue>> myfilter = new Predicate<TreeNode<PropertyId, PropertyValue>>() {
			@Override
			public boolean apply(TreeNode<PropertyId, PropertyValue> treeNode) {
				if (filters.size() == 0)
					return true;
				for (Predicate<ReadNode> filter : filters) {
					if (!filter.apply(ReadNodeImpl.load(session, treeNode)))
						return false;
				}
				return true;
			}
		};

		if (sorts.size() == 0) { // no sort
			Iterator<TreeNode<PropertyId, PropertyValue>> iterator =  Iterators.limit(Iterators.filter(oriIter, myfilter), skip + offset);
			Iterators.advance(iterator, skip) ;
			return new ReloadIterator(session, iterator);
		} 

		// List<TreeNode<PropertyId, PropertyValue>> sorted = Ordering.from(comparator).greatestOf(new Iterable<TreeNode<PropertyId, PropertyValue>>(){
		// @Override
		// public Iterator<TreeNode<PropertyId, PropertyValue>> iterator() {
		// return oriIter;
		// }
		// }, skip + offset);
		// return new ReloadIterator(session, sorted.subList(skip, sorted.size()).iterator()) ;

		List<TreeNode<PropertyId, PropertyValue>> result = SortUtil.selectTopN(oriIter, myfilter, mycomparator, skip + offset);
		return new ReloadIterator(session, result.subList(skip, result.size()).iterator());

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
