package net.ion.craken.node.crud;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import net.ion.craken.node.AbstractChildren;
import net.ion.craken.node.IteratorList;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.SortElement;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.WriteSession;
import net.ion.craken.node.crud.tree.Fqn;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.util.WriteChildrenEachs;
import net.ion.framework.util.ListUtil;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

public class WriteChildren  extends AbstractChildren<WriteNode, WriteChildren> implements Iterable<WriteNode> {


	private int skip = 0 ;
	private int offset = 1000;
	private List<SortElement> sorts = ListUtil.newList() ;
	private List<Predicate<WriteNode>> filters = ListUtil.newList();;
	
	private final WriteSession session ;
	private final Fqn parent;
	private final Iterator<Fqn> children ;


	WriteChildren(WriteSession session, Fqn parentFqn, Iterator<Fqn> children){
		this.session = session ;
		this.parent = parentFqn ;
		this.children = children;
	}
	
	public WriteChildren skip(int skip){
		this.skip = skip ;
		return this ;
	}
	
	public WriteChildren offset(int offset){
		this.offset = offset ;
		return this ;
	}
	
	public WriteChildren ascending(String propId){
		sorts.add(new SortElement(propId, true)) ;
		return this ;
	}

	public WriteChildren descending(String propId){
		sorts.add(new SortElement(propId, false)) ;
		return this ;
	}
	
	public WriteChildren filter(Predicate<WriteNode> filter){
		filters.add(filter) ;
		return this ;
	}

	

	public <T> T eachNode(WriteChildrenEach<T> reach){
		List<WriteNode> targets = readChildren();
		WriteChildrenIterator citer = WriteChildrenIterator.create(session, targets);
		T result = reach.handle(citer) ;
		return result ;
	}
	
	private List<WriteNode> readChildren() {
		
		List<WriteNode> listNode = ListUtil.newList() ;
		Predicate<WriteNode> andFilters = Predicates.and(filters) ;
		while(children.hasNext()){
			Fqn tn = children.next() ;
			WriteNode read = WriteNodeImpl.loadTo(session, tn);
			if (andFilters.apply(read)) listNode.add(read) ;
		}
		
		if (sorts.size() > 0) {
			Comparator<WriteNode> mycomparator = new Comparator<WriteNode>() {
				@Override
				public int compare(WriteNode left, WriteNode right) {

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
			Collections.sort(listNode, mycomparator);
		}
		
		List<WriteNode> result = listNode.subList(skip, Math.min(skip + offset, listNode.size())) ;
		return result ;
	}

	public WriteNode firstNode() {
		return eachNode(WriteChildrenEachs.FIRSTNODE);
	}

	
	public List<WriteNode> toList() {
		return eachNode(WriteChildrenEachs.LIST);
	}
	
	public IteratorList<WriteNode> iterator(){
		return eachNode(WriteChildrenEachs.ITERATOR) ;
	}

	public void debugPrint() {
		eachNode(WriteChildrenEachs.DEBUG);
	}

	public int count() {
		return eachNode(WriteChildrenEachs.COUNT);
	}

	public PropertyValue min(final String propId) {
		return eachNode(new WriteChildrenEach<PropertyValue>(){
			@Override
			public PropertyValue handle(WriteChildrenIterator citer) {
				List<PropertyValue> store = ListUtil.newList() ;
				while(citer.hasNext()){
					WriteNode rnode = citer.next() ;
					store.add(rnode.property(propId)) ;
				}
				
				Collections.sort(store);
				return store.size() > 0 ? store.get(0) : PropertyValue.NotFound;
			}
		}) ;
	}

	public PropertyValue max(final String propId) {
		return eachNode(new WriteChildrenEach<PropertyValue>(){
			@Override
			public PropertyValue handle(WriteChildrenIterator citer) {
				List<PropertyValue> store = ListUtil.newList() ;
				while(citer.hasNext()){
					WriteNode rnode = citer.next() ;
					store.add(rnode.property(propId)) ;
				}
				
				Collections.sort(store);
				Collections.reverse(store);
				return store.size() > 0 ? store.get(0) : PropertyValue.NotFound;
			}
		}) ;
	}

}


