package net.ion.craken.mr;

import net.ion.craken.tree.PropertyId;
import net.ion.craken.tree.PropertyValue;
import net.ion.craken.tree.TreeNodeKey;

import org.infinispan.atomic.AtomicMap;
import org.infinispan.distexec.mapreduce.Mapper;
import org.infinispan.distexec.mapreduce.Reducer;

public interface NodeMapReduce<Ri, Rv> extends Mapper<TreeNodeKey, AtomicMap<PropertyId, PropertyValue>, Ri, Rv>, Reducer<Ri, Rv> {

}
