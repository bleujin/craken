package net.ion.craken;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.plaf.ListUI;

import net.ion.bleujin.TestCreate.EntryListener;
import net.ion.craken.simple.SimpleKeyFactory;
import net.ion.craken.simple.SimpleEntry;
import net.ion.framework.db.Page;
import net.ion.framework.util.Debug;
import net.ion.framework.util.ListUtil;

import org.infinispan.Cache;

public class LegContainer<E extends AbstractEntry> {

	private final Cache<EntryKey, E> cache;
	private Class<? extends AbstractEntry> clz;

	private LegContainer(Cache<EntryKey, E> cache, Class<? extends AbstractEntry> clz) {
		this.cache = cache;
		this.clz = clz;
	}

	static <E extends AbstractEntry> LegContainer<E> create(Cache<EntryKey, E> cache, Class<? extends AbstractEntry> clz) {
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

	public Set<EntryKey> keySet() {
		return cache.keySet();
	}
	
	public Set<Entry<EntryKey, E>> entrySet(){
		return cache.entrySet() ;
	}

	public E newInstance(Object keyInstance) throws InstantiationException, IllegalAccessException, IllegalArgumentException, SecurityException, InvocationTargetException, NoSuchMethodException {
		Constructor<? extends AbstractEntry> con = null;
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
		if (key instanceof EntryKey) {
			E result = cache.get(key);
			if (result == null) return null ;
			result.setContainer(this);
			return result;
		} else {
			return findByKey(SimpleKeyFactory.create(key)) ;
		}
	}

	public E findOne(EntryFilter<E> entryFilter) {
		for (EntryKey key : keySet()) {
			E entry = cache.get(key);
			if (entryFilter.filter(entry)) {
				return entry ;
			}
		}
		
		return null ;
	}

	public List<E> find(EntryFilter<E> entryFilter) {
		return find(entryFilter, Page.HUNDRED) ;
	}
	public List<E> find(EntryFilter<E> entryFilter, Page page) {
		List<E> result = ListUtil.newList() ;
		
		int foundCount = 0 ;
		for (EntryKey key : keySet()) {
			E entry = cache.get(key);
			if (! entryFilter.filter(entry)) continue ;
			foundCount++ ;
			if (foundCount <= page.getStartLoc()) {
				continue ;
			}
			
			if (foundCount > page.getEndLoc()){
				break ;
			}
			result.add(entry) ;
		}
		
		return result ;
	}

	public List<E> findAll() {
		List<E> result = ListUtil.newList() ;
		
		for (EntryKey key : keySet()) {
			result.add(cache.get(key)) ;
		}
		return result ;
	}

	public E remove(EntryKey key) {
		return cache.remove(key) ;
	}

}
