package net.ion.craken.node.crud;

import java.sql.SQLException;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.ion.craken.expression.ExpressionParser;
import net.ion.craken.expression.SelectProjection;
import net.ion.craken.expression.TerminalParser;
import net.ion.craken.node.AbstractChildren;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.SortElement;
import net.ion.craken.node.convert.rows.AdNodeRows;
import net.ion.craken.node.crud.util.ReadChildrenEachs;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.db.Page;
import net.ion.framework.db.Rows;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.StringUtil;
import net.ion.rosetta.Parser;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterators;

public class ReadChildren extends AbstractChildren<ReadNode, ReadChildren> implements Iterable<ReadNode>{

	private int skip = 0;
	private int offset = 1000;
	private List<SortElement> sorts ;
	private List<Predicate<ReadNode>> filters ;

	private final ReadSession session;
	private TreeNode source; // parent or refsource
	private Iterator<TreeNode> treeNodes ;

	ReadChildren(ReadSession session, TreeNode source, Iterator<TreeNode> treeNodes) {
		this.session = session;
		this.source = source ;
		this.treeNodes = treeNodes ;
		this.sorts = ListUtil.newList();
		this.filters = ListUtil.newList();
	}

	public ReadChildren skip(int skip) {
		this.skip = skip;
		return this;
	}

	public ReadChildren offset(int offset) {
		this.offset = offset;
		return this;
	}
	
	protected TreeNode source(){
		return source ;
	}
	
	protected ReadSession session(){
		return session ;
	}
	
	protected Iterator<TreeNode> treeNodes(){
		return treeNodes ;
	}
	
	protected int skip(){
		return skip ;
	}
	protected int offset(){
		return offset ;
	}
	protected List<SortElement> sorts(){
		return sorts ;
	}
	protected List<Predicate<ReadNode>> filters(){
		return filters ;
	}
	
	
	public <T> T eachNode(ReadChildrenEach<T> reach){
		List<ReadNode> targets = readChildren();
		ReadChildrenIterator citer = ReadChildrenIterator.create(session, targets);
		T result = reach.handle(citer) ;
		return result ;
	}
	
	
	protected List<ReadNode> readChildren() {
		
		List<ReadNode> listNode = ListUtil.newList() ;
		
		Predicate<ReadNode> andFilters = Predicates.and(filters) ; 

		while(treeNodes.hasNext()){
			TreeNode tn = treeNodes.next() ;
			ReadNode read = ReadNodeImpl.load(session, tn);
			if (andFilters.apply(read)) listNode.add(read) ; // apply filter
		}
		
		if (sorts.size() > 0) {
			Comparator<ReadNode> mycomparator = new Comparator<ReadNode>() {
				@Override
				public int compare(ReadNode left, ReadNode right) {

					for (SortElement sele : sorts) {
						PropertyValue leftProperty = left.property(sele.propid());
						PropertyValue rightProperty = right.property(sele.propid());

						if (leftProperty == PropertyValue.NotFound && rightProperty == PropertyValue.NotFound) return 0;
						if (leftProperty == PropertyValue.NotFound) return -1 * (sele.ascending() ? 1 : -1);
						if (rightProperty == PropertyValue.NotFound) return 1 * (sele.ascending() ? 1 : -1);

						int result = leftProperty.compareTo(rightProperty) * (sele.ascending() ? 1 : -1);
						if (result != 0) return result ;
					}
					return 0;
				}
			};
			Collections.sort(listNode, mycomparator); // apply sort
		}
		
		if (listNode.size() < skip) return ListUtil.EMPTY ;
		
		List<ReadNode> result = listNode.subList(skip, Math.min(skip + offset, listNode.size())) ; // apply skip & offset
		return result ;
	}

	public ReadChildren ascending(String propId) {
		sorts.add(new SortElement(propId, true));
		return this;
	}

	public ReadChildren descending(String propId) {
		sorts.add(new SortElement(propId, false));
		return this;
	}

	public ReadChildren filter(Predicate<ReadNode> filter) {
		filters.add(filter);
		return this;
	}


	@Deprecated
	public Rows toRows(String... cols) throws SQLException {
		return toAdRows(StringUtil.join(cols, ',')) ;
	}

	@Deprecated
	public Rows toRows(Page _page, String... cols) throws SQLException {
		return toAdRows(_page, StringUtil.join(cols, ',')) ;
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
		
		Iterator<ReadNode> iter = readChildren().iterator() ;
		Iterators.skip(iter, page.getSkipOnScreen()) ;
		Iterator<ReadNode> limitIter = Iterators.limit(iter, page.getOffsetOnScreen());
		
		List<ReadNode> screenList = ListUtil.newList() ;
		while(limitIter.hasNext()){
			screenList.add(limitIter.next()) ;
		}

		int count = screenList.size();
		Page pageOnScreen = Page.create(page.getListNum(), page.getPageNo() % page.getScreenCount() == 0 ? page.getScreenCount() : page.getPageNo() % page.getScreenCount(), page.getScreenCount()) ;
		return AdNodeRows.create(session,  pageOnScreen.subList(screenList).iterator(), sp, count, "cnt");
	}

	public ReadNode firstNode() {
		return eachNode(ReadChildrenEachs.FIRSTNODE);
	}

	
	public List<ReadNode> toList() {
		return eachNode(ReadChildrenEachs.LIST);
	}
	
	public IteratorList<ReadNode> iterator(){
		return eachNode(ReadChildrenEachs.ITERATOR) ;
	}

	public void debugPrint() {
		eachNode(ReadChildrenEachs.DEBUG);
	}

	public int count() {
		return eachNode(ReadChildrenEachs.COUNT);
	}

	public WalkReadChildren asTreeChildren() {
		return (WalkReadChildren)this;
	}



}


