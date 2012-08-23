package net.ion.craken;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.plaf.ListUI;

import net.ion.craken.TestCreate.EntryListener;
import net.ion.craken.simple.SimpleKeyFactory;
import net.ion.craken.simple.SimpleMapNode;
import net.ion.framework.db.Page;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

import org.infinispan.Cache;

public class LegContainer<E extends AbstractNode> {

	private final Cache<NodeKey, E> cache;
	private Class<? extends AbstractNode> clz;

	private LegContainer(Cache<NodeKey, E> cache, Class<? extends AbstractNode> clz) {
		this.cache = cache;
		this.clz = clz;
	}

	static <E extends AbstractNode> LegContainer<E> create(Cache<NodeKey, E> cache, Class<? extends AbstractNode> clz) {
		return new LegContainer<E>(cache, clz);
	}

	public LegContainer<E> putNode(E dataNode) {
		cache.put(dataNode.key(), dataNode);
		return this;
	}

	public LegContainer<E> addListener(Object listener) {
		cache.addListener(listener);
		return this;
	}

	public Set<NodeKey> keySet() {
		return cache.keySet();
	}
	
	public Set<Entry<NodeKey, E>> entrySet(){
		return cache.entrySet() ;
	}

	public E newInstance(Object keyInstance) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException {
		Constructor<? extends AbstractNode> con = null;
		try {
			con = clz.getDeclaredConstructor(keyInstance.getClass());
		} catch(NoSuchMethodException ex){
			con =  clz.getDeclaredConstructor(Object.class);
		}
		con.setAccessible(true);

		E newInstance = (E) con.newInstance(keyInstance);
		newInstance.setContainer(this);

		return newInstance;
	}

	public E findByKey(Object key) {
		if (key instanceof NodeKey) {
			E result = cache.get(key);
			if (result == null) return null ;
			result.setContainer(this);
			return result;
		} else {
			return findByKey(SimpleKeyFactory.create(key)) ;
		}
	}

	public E findOne(NodeFilter<E> nodeFilter) {
		for (NodeKey key : keySet()) {
			E node = cache.get(key);
			if (nodeFilter.filter(node)) {
				return node ;
			}
		}
		
		return null ;
	}

	public List<E> find(NodeFilter<E> nodeFilter) {
		return find(nodeFilter, Page.HUNDRED) ;
	}
	public List<E> find(NodeFilter<E> nodeFilter, Page page) {
		List<E> result = ListUtil.newList() ;
		
		int foundCount = 0 ;
		for (NodeKey key : keySet()) {
			E node = cache.get(key);
			if (! nodeFilter.filter(node)) continue ;
			foundCount++ ;
			if (foundCount <= page.getStartLoc()) {
				continue ;
			}
			
			if (foundCount > page.getEndLoc()){
				break ;
			}
			result.add(node) ;
		}
		
		return result ;
	}

}
