package net.ion.craken.mr;

import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;

import org.infinispan.Cache;
import org.infinispan.distexec.mapreduce.MapReduceTask;

public class NodeMapReduceTask<Ri, Rv> extends MapReduceTask<PropertyId, PropertyValue, Ri, Rv>{

	public NodeMapReduceTask(Cache<PropertyId, PropertyValue> cache) {
		super(cache);
	}

}
