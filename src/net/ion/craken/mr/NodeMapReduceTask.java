package net.ion.craken.mr;

import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.distexec.mapreduce.MapReduceTask;

public class NodeMapReduceTask<Ri, Rv> extends MapReduceTask<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>, Ri, Rv>{

	public NodeMapReduceTask(Cache<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>> cache) {
		super(cache);
	}

}
