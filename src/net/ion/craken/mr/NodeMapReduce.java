package net.ion.craken.mr;

import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.PropertyValue;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;

public interface NodeMapReduce<Ri, Rv> extends Mapper<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>, Ri, Rv>, Reducer<Ri, Rv> {

}
