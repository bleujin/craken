package net.ion.craken;

import java.util.Set;

import net.ion.craken.TestCreate.EntryListener;

import org.infinispan.Cache;

public class CrakenLeg {

	private final Cache<NodeKey, DataNode> cache ;
	private CrakenLeg(Cache<NodeKey, DataNode> cache){
		this.cache = cache ;
	}
	
	static CrakenLeg create(Cache<NodeKey, DataNode> cache) {
		return new CrakenLeg(cache);
	}

	public CrakenLeg putNode(DataNode dataNode) {
		cache.put(dataNode.getKey(), dataNode) ;
		return this ;
	}

	public CrakenLeg addListener(Object listener) {
		cache.addListener(listener) ;
		return this ;
	}

	public Set<NodeKey> keySet() {
		return cache.keySet() ;
	}

}
