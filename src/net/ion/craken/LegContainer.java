package net.ion.craken;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import net.ion.craken.simple.EmanonKey;
import net.ion.framework.db.Page;
import net.ion.framework.util.ListUtil;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.lifecycle.ComponentStatus;

public class LegContainer<E extends AbstractEntry> {

	private final EntryFilter<E> allFilter = new EntryFilter<E>() {
		@Override
		public boolean filter(E entry) {
			return true;
		}
	};
	
	private final Craken craken ;
	private final Cache<EntryKey, E> cache;
	private Class<E> clz;

	private LegContainer(Craken craken, Cache<EntryKey, E> cache, Class<E> clz) {
		this.craken = craken ;
		this.cache = cache;
		this.clz = clz;
	}

	static <E extends AbstractEntry> LegContainer<E> create(Craken craken, Cache<EntryKey, E> cache, Class<E> clz) {
		return new LegContainer<E>(craken, cache, clz);
	}

	public LegContainer<E> putNode(E dataNode) {
		cache.put(dataNode.key(), dataNode);
		return this;
	}

	public LegContainer<E> addListener(Object listener) {
		cache.addListener(listener);
		return this;
	}
	
	public Craken getCraken(){
		return craken ;
	}
	

	public Set<EntryKey> keySet() {
		return cache.keySet();
	}

	public Set<Entry<EntryKey, E>> entrySet() {
		return cache.entrySet();
	}

	public E newInstance(Object keyInstance) {
		try {
			Constructor<E> con = null;
			try {
				con = clz.getDeclaredConstructor(keyInstance.getClass());
			} catch (NoSuchMethodException ex) {
				con = clz.getDeclaredConstructor(Object.class);
			}
			con.setAccessible(true);

			E newInstance = (E) con.newInstance(keyInstance);
			newInstance.setContainer(this);
			return newInstance;
		} catch (NoSuchMethodException ex) {
			throw new IllegalArgumentException(ex);
		} catch (IllegalArgumentException ex) {
			throw ex ;
		} catch (InstantiationException ex) {
			throw new IllegalArgumentException(ex);
		} catch (IllegalAccessException ex) {
			throw new IllegalArgumentException(ex);
		} catch (InvocationTargetException ex) {
			throw new IllegalArgumentException(ex);
		}
	}
	
	public E mergeInstance(Object keyInstance){
		E found = findByKey(keyInstance) ;
		if (found == null) {
			found = newInstance(keyInstance) ;
		}
		return found ;
	}
	

	public boolean containsKey(Object key){
		return key != null && cache.containsKey(transKey(key)) ;
	}
	
	private EntryKey transKey(Object key) {
		if (key instanceof EntryKey) {
			return (EntryKey)key ;
		} else {
			return EmanonKey.create(key) ;
		}
	}

	public E findByKey(Object key) {
		if (key == null) return null ;
		E result = cache.get(transKey(key));
		if (result == null)
			return null;
		result.setContainer(this);
		return result;
	}

	public E findOneInMemory(EntryFilter<E> entryFilter) {
		for (EntryKey key : keySet()) {
			E entry = cache.get(key);
			if (entryFilter.filter(entry)) {
				return entry;
			}
		}

		return null;
	}

	@Deprecated
	public List<E> findInMemory(EntryFilter<E> entryFilter) {
		return find(entryFilter, Page.HUNDRED);
	}

	public List<E> find(EntryFilter<E> entryFilter, Page page) {
		List<E> result = ListUtil.newList();

		int foundCount = 0;
		for (EntryKey key : keySet()) {
			E entry = cache.get(key);
			if (!entryFilter.filter(entry))
				continue;
			foundCount++;
			if (foundCount <= page.getStartLoc()) {
				continue;
			}

			if (foundCount > page.getEndLoc()) {
				break;
			}
			result.add(entry);
		}

		return result;
	}

	public List<E> findAllInMemory() {
		List<E> result = ListUtil.newList();

		for (EntryKey key : keySet()) {
			result.add(cache.get(key));
		}
		return result;
	}

	private E remove(EntryKey key) {
		return cache.remove(key);
	}

	
	
	public E findOne() {
		return findOneInMemory(allFilter);
	}

	public E removeByKey(Object key) {
		return remove(transKey(key)) ;
	}
	
	public Configuration getCacheConfiguration(){
		return cache.getCacheConfiguration() ;
	}

	public void clear(){
		cache.clear() ;
	}
	
	public void stop() {
		cache.stop() ;
	}

	public ComponentStatus state(){
		return cache.getStatus() ;
	}
	
}
