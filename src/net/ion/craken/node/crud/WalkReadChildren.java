package net.ion.craken.node.crud;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.ion.craken.node.ReadNode;
import net.ion.craken.node.ReadSession;
import net.ion.craken.node.SortElement;
import net.ion.craken.node.crud.util.TraversalStrategy;
import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.framework.util.ListUtil;

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
		if (includeSelf) result.add(WalkReadNode.create(session(), source(), 0)) ;
		
		this.andFilters = Predicates.and(filters()) ; 
		
		if (strategy == TraversalStrategy.BreadthFirst) this.buildBreadthList(result, treeNodes(), 1);
		else this.buildDepthList(result, treeNodes(), 1) ;
		
		
		return result.subList(skip(), Math.min(skip() + offset(), result.size())) ;
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

	
	private List<TreeNode> buildDepthList(LinkedList<ReadNode> list, Iterator<TreeNode> children, int level) {
		if (!children.hasNext()) return ListUtil.EMPTY ;
		
		Iterator<TreeNode> sortedChildren = sort(children) ;
		List<TreeNode> inner = ListUtil.newList() ;
		while(sortedChildren.hasNext()){
			TreeNode child = sortedChildren.next();
        	WalkReadNode target = WalkReadNode.create(session(), child, level);
			if (! andFilters.apply(target)) continue ;
			
			list.add(target) ;
			inner.addAll(child.getChildren()) ;
		}

		return buildDepthList(list, inner.iterator(), level++) ;
	}


	private void buildBreadthList(LinkedList<ReadNode> list, Iterator<TreeNode> children, int level) {
		Iterator<TreeNode> sortedChildren = sort(children) ;
        while(sortedChildren.hasNext()){
        	TreeNode child = sortedChildren.next();
        	WalkReadNode target = WalkReadNode.create(session(), child, level);
			if (andFilters.apply(target)) list.add(target) ;
			
			this.buildBreadthList(list, child.getChildren().iterator(), level++) ;
        }
    }


	private Iterator<TreeNode> sort(Iterator<TreeNode> children) {
		if (sorts().size() > 0) {
			List<TreeNode> childrenList = Lists.newArrayList(children) ;
			Comparator<TreeNode> mycomparator = new Comparator<TreeNode>() {
				@Override
				public int compare(TreeNode left, TreeNode right) {

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


