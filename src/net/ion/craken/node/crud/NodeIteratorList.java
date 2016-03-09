package net.ion.craken.node.crud;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import net.ion.craken.node.IteratorList;
import net.ion.craken.node.NodeCommon;
import net.ion.craken.node.ReadNode;
import net.ion.craken.node.WriteNode;
import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.framework.util.ListUtil;
import net.ion.framework.util.SetUtil;

public abstract class NodeIteratorList<T extends NodeCommon> extends IteratorList<T>{

	public void debugPrint(){
		for(NodeCommon n : toList()){
			n.debugPrint(); 
		}
	}
	
	public static NodeIteratorList<ReadNode> ReadRefs(final ReadNode rnode, String refName){
		PropertyId referId = PropertyId.refer(refName);
		final Set values = rnode.hasPropertyId(referId) ? rnode.propertyId(referId).asSet() : SetUtil.EMPTY_SET;
		final Iterator<String> iter = values.iterator();

		return new NodeIteratorList<ReadNode>() {
			@Override
			public List<ReadNode> toList() {
				List<ReadNode> result = ListUtil.newList();
				while (iter.hasNext()) {
					final ReadNode next = rnode.session().ghostBy(iter.next());
					if (next.isGhost()) continue ;
					result.add(next);
				}
				return Collections.unmodifiableList(result);
			}
			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public ReadNode next() {
				return rnode.session().ghostBy(iter.next());
			}

			@Override
			public Iterator<ReadNode> iterator() {
				return this;
			}

			public int count() {
				return values.size();
			}
		};
	}

	public static NodeIteratorList<WriteNode> WriteRefs(final WriteNode wnode, String refName) {
		PropertyId referId = PropertyId.refer(refName);
		final Set values = wnode.hasPropertyId(referId) ? wnode.propertyId(referId).asSet() : SetUtil.EMPTY;
		final Iterator<String> iter = values.iterator();

		return new NodeIteratorList<WriteNode>() {
			@Override
			public List<WriteNode> toList() {
				List<WriteNode> result = ListUtil.newList();
				while (iter.hasNext()) {
					final ReadNode next = wnode.session().readSession().ghostBy(iter.next());
					if (next.isGhost()) continue ;
					result.add(wnode.session().pathBy(iter.next()));
				}
				return Collections.unmodifiableList(result);
			}

			@Override
			public boolean hasNext() {
				return iter.hasNext();
			}

			@Override
			public WriteNode next() {
				return wnode.session().pathBy(iter.next());
			}

			@Override
			public Iterator<WriteNode> iterator() {
				return this;
			}

			public int count() {
				return values.size();
			}
		};

	}
}
