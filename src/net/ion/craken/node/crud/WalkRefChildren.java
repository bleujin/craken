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
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.TreeNode;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.util.TraversalStrategy;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;

public class WalkRefChildren extends ReadChildren {

	private TraversalStrategy strategy = TraversalStrategy.BreadthFirst ;
	private boolean includeSelf = false;
	private Predicate<ReadNode> andFilters;
	private final String refName;
	private int loopLimit;
	
	WalkRefChildren(ReadSession session, Fqn sourceFqn, String refName, Iterator<Fqn> refchildrenFqn) {
		super(session, sourceFqn, refchildrenFqn) ;
		this.refName = refName ;
		this.loopLimit = 10 ;
	}
	
	public WalkRefChildren loopLimit(int loopLimit){
		this.loopLimit = loopLimit ;
		
		return this;
	}

	protected List<ReadNode> readChildren() {
		LinkedList<ReadNode> result = new LinkedList<ReadNode>();
		WalkReadNode rootFrom = WalkReadNode.create(session(), null, sourceFqn(), 0);
		if (includeSelf) result.add(rootFrom) ;
		
		this.andFilters = Predicates.and(filters()) ; 
		
		if (strategy == TraversalStrategy.BreadthFirst) this.buildBreadthList(result, makeWalk(rootFrom, treeFqn(), 1), 1);
		else this.buildDepthList(result, makeWalk(rootFrom, treeFqn(), 1), 1) ;
		
		
		return result.subList(skip(), Math.min(skip() + offset(), result.size())) ;
	}
	
	
	private Iterator<WalkReadNode> makeWalk(WalkReadNode rootFrom, Iterator<Fqn> treeFqn, int level) {
		Set<WalkReadNode> result = SetUtil.newSet() ;
		while(treeFqn.hasNext()){
			result.add(WalkReadNode.create(session(), rootFrom, treeFqn.next(), level)) ;
		}
		return result.iterator();
	}

	private List<WalkReadNode> readTreeChildren(){
		List<WalkReadNode> result = ListUtil.newList() ;
		for(ReadNode rnode : readChildren()){
			result.add((WalkReadNode)rnode) ;
		}
		
		return result ;
	}
	

	public <T> T eachTreeNode(WalkChildrenEach<T> trcEach) {
		WalkChildrenIterator trcIterable = WalkChildrenIterator.create(session(), readTreeChildren()) ; 
		return trcEach.handle(trcIterable) ;
	}

	
	
	
	
	private List<TreeNode> buildBreadthList(LinkedList<ReadNode> list, Iterator<WalkReadNode> children, int level) {
		if (loopLimit-- <= 0) return ListUtil.EMPTY;
		if (!children.hasNext()) return ListUtil.EMPTY;
		
		Iterator<WalkReadNode> sortedChildren = sort(children) ;
		List<WalkReadNode> inner = ListUtil.newList() ;
		
		while(sortedChildren.hasNext()){
			WalkReadNode child = sortedChildren.next();
			
        	WalkReadNode target = WalkReadNode.create(session(), child.from(), child.fqn(), level);
			if (! andFilters.apply(target)) continue ;
			
			list.add(target) ;
			inner.addAll(child.getReferences(refName)) ;
		}

		return buildBreadthList(list, inner.iterator(), ++level) ;
	}


	private void buildDepthList(LinkedList<ReadNode> list, Iterator<WalkReadNode> children, int level) {
		if (loopLimit-- <= 0) return ;
		
		Iterator<WalkReadNode> sortedChildren = sort(children) ;
        while(sortedChildren.hasNext()){
        	WalkReadNode child = sortedChildren.next();
        	WalkReadNode target = WalkReadNode.create(session(), child.from(), child.fqn(), level);
			if (! andFilters.apply(target)) continue ;
			
			list.add(target) ;
			this.buildDepthList(list, child.getReferences(refName).iterator(), (level+1)) ;
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


	public WalkRefChildren strategy(TraversalStrategy strategy) {
		this.strategy = strategy ;
		
		return this;
	}

	public WalkRefChildren includeSelf(boolean includeSelf){
		this.includeSelf = includeSelf ;
		return this ;
	}
	
	public void debugPrint() {
		eachTreeNode(new WalkChildrenEach<Void>() {

			@Override
			public <T> T handle(WalkChildrenIterator trc) {
				Iterator<WalkReadNode> iter = trc.iterator() ;
				while(iter.hasNext()){
					WalkReadNode wnode = iter.next();
					
					Debug.line(wnode.level(), wnode) ;
				}
				return null;
			}
		});
	}

}
