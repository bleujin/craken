package net.ion.craken.node.crud;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.SortElement;
import net.ion.craken.node.crud.util.TraversalStrategy;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
public class WalkReadChildren extends ReadChildren{


	private TraversalStrategy strategy = TraversalStrategy.BreadthFirst ;
	private boolean includeSelf = false;
	private Predicate<ReadNode> andFilters;
	
	
	WalkReadChildren(ReadSession session, TreeNode source, Iterator<TreeNode> children) {
		super(session, source, children) ;
	}
	
	protected List<ReadNode> readChildren() {
		LinkedList<ReadNode> result = new LinkedList<ReadNode>();
		WalkReadNode rootFrom = WalkReadNode.create(session(), null, source(), 0);
		if (includeSelf) {
			result.add(rootFrom) ;
		}
		
		this.andFilters = Predicates.and(filters()) ; 
		
		if (strategy == TraversalStrategy.BreadthFirst) this.buildBreadthList(result, makeWalk(rootFrom, treeNodes(), 1), 1);
		else this.buildDepthList(result, makeWalk(rootFrom, treeNodes(), 1), 1) ;
		
		
		return result.subList(skip(), Math.min(skip() + offset(), result.size())) ;
	}
	
	
	private List<WalkReadNode> readTreeChildren(){
		List<WalkReadNode> result = ListUtil.newList() ;
		for(ReadNode rnode : readChildren()){
			result.add((WalkReadNode)rnode) ;
		}
		
		return result ;
	}

	private Iterator<WalkReadNode> makeWalk(WalkReadNode rootFrom, Iterator<TreeNode> treeNodes, int level) {
		Set<WalkReadNode> result = SetUtil.newSet() ;
		while(treeNodes.hasNext()){
			result.add(WalkReadNode.create(session(), rootFrom, treeNodes.next(), level)) ;
		}
		return result.iterator();
	}

	

	public <T> T eachTreeNode(WalkChildrenEach<T> trcEach) {
		WalkChildrenIterator trcIterable = WalkChildrenIterator.create(session(), readTreeChildren()) ; 
		return trcEach.handle(trcIterable) ;
	}

	
	private List<TreeNode> buildBreadthList(LinkedList<ReadNode> list, Iterator<WalkReadNode> children, int level) {
		if (!children.hasNext()) return ListUtil.EMPTY ;
		
		Iterator<WalkReadNode> sortedChildren = sort(children) ;
		List<WalkReadNode> inner = ListUtil.newList() ;
		while(sortedChildren.hasNext()){
			WalkReadNode child = sortedChildren.next();
        	WalkReadNode target = WalkReadNode.create(session(), child.from(), child.treeNode(), level);
			if (! andFilters.apply(target)) continue ;
			
			list.add(target) ;
			inner.addAll(child.getChildren()) ;
		}

		return buildBreadthList(list, inner.iterator(), ++level) ;
	}


	private void buildDepthList(LinkedList<ReadNode> list, Iterator<WalkReadNode> children, int level) {
		Iterator<WalkReadNode> sortedChildren = sort(children) ;
        while(sortedChildren.hasNext()){
        	WalkReadNode child = sortedChildren.next();
        	WalkReadNode target = WalkReadNode.create(session(), child.from(), child.treeNode(), level);
			if (andFilters.apply(target)) list.add(target) ;
			
			this.buildDepthList(list, child.getChildren().iterator(), (level+1)) ;
        }
    }


	private Iterator<WalkReadNode> sort(Iterator<WalkReadNode> children) {
		if (sorts().size() > 0) {
			List<WalkReadNode> childrenList = Lists.newArrayList(children) ;
			Comparator<WalkReadNode> mycomparator = new Comparator<WalkReadNode>() {
				@Override
				public int compare(WalkReadNode left, WalkReadNode right) {

					for (SortElement sele : sorts()) {
						PropertyId spid = PropertyId.fromIdString(sele.propid()) ;
						PropertyValue leftProperty = left.get(spid);
						PropertyValue rightProperty = right.get(spid);

						if (leftProperty == null && rightProperty == null) return 0;
						if (leftProperty == null) return -1 * (sele.ascending() ? 1 : -1);
						if (rightProperty == null) return 1 * (sele.ascending() ? 1 : -1);

						int result = leftProperty.compareTo(rightProperty) * (sele.ascending() ? 1 : -1);
						if (result != 0) return result ;
					}
					return 0;
				}
			};
			Collections.sort(childrenList, mycomparator); // apply sort
			return childrenList.iterator() ;
		}
		return children;
	}


	public WalkReadChildren strategy(TraversalStrategy strategy) {
		this.strategy = strategy ;
		
		return this;
	}

	public WalkReadChildren includeSelf(boolean includeSelf){
		this.includeSelf = includeSelf ;
		return this ;
	}
	
	
	
}


