package net.ion.craken.mr;

import net.ion.craken.tree.TreeNodeKey;

import org.infinispan.Cache;
import org.infinispan.atomic.AtomicMap;
import org.infinispan.distexec.mapreduce.MapReduceTask;

public class NodeMapReduceTask<Ri, Rv> extends MapReduceTask<TreeNodeKey, AtomicMap<?, ?>, Ri, Rv>{

	public NodeMapReduceTask(Cache<TreeNodeKey, AtomicMap<?, ?>> cache) {
		super(cache);
	}

}
