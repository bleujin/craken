package net.ion.craken.loaders;

import infinispan.org.codehaus.jackson.map.deser.impl.PropertyValue;

import java.util.Map;
import java.util.concurrent.Executor;

import net.ion.craken.node.crud.tree.impl.PropertyId;
import net.ion.craken.node.crud.tree.impl.TreeNodeKey;
import net.ion.framework.util.Debug;

import org.infinispan.filter.KeyFilter;
import org.infinispan.marshall.core.MarshalledEntry;
import org.infinispan.notifications.Listener;
import org.infinispan.persistence.spi.AdvancedLoadWriteStore;
import org.infinispan.persistence.spi.InitializationContext;

@Listener
public class GridStore implements AdvancedLoadWriteStore<TreeNodeKey, Map<PropertyId, PropertyValue>> {

	@Override
	public boolean contains(Object arg0) {
		Debug.line("contains");
		return false;
	}

	@Override
	public void init(InitializationContext icontext) {
		Debug.line("init");
	}

	@Override
	public MarshalledEntry<TreeNodeKey, Map<PropertyId, PropertyValue>> load(Object obj) {
		Debug.line("load", obj);
		return null;
	}

	@Override
	public void start() {
		Debug.line("start");

	}

	@Override
	public void stop() {
		Debug.line("stop");
	}

	@Override
	public boolean delete(Object obj) {
		Debug.line("delete", obj);

		return false;
	}

	@Override
	public void write(MarshalledEntry<? extends TreeNodeKey, ? extends Map<PropertyId, PropertyValue>> entry) {
		Debug.line("write", entry);
	}

	@Override
	public void process(KeyFilter<? super TreeNodeKey> filter, CacheLoaderTask<TreeNodeKey, Map<PropertyId, PropertyValue>> tasks, Executor ex, final boolean fetchValue, final boolean fetchMetadata) {
		Debug.line("write", filter, tasks, ex, fetchValue);
	}

	@Override
	public int size() {
		Debug.line("size");
		return 0;
	}

	@Override
	public void clear() {
		Debug.line("clear");
	}

	@Override
	public void purge(Executor arg0, org.infinispan.persistence.spi.AdvancedCacheWriter.PurgeListener<? super TreeNodeKey> arg1) {
		Debug.line("purge");
	}

}
