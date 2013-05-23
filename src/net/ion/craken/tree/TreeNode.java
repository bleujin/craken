package net.ion.craken.tree;

import java.util.Map;
import java.util.Set;

import org.infinispan.Cache;
import org.infinispan.context.Flag;


// @ThreadSafe
public interface TreeNode<K, V> {

	TreeNode<K, V> getParent();

	TreeNode<K, V> getParent(Flag... flags);

	Set<TreeNode<K, V>> getChildren();

	Set<TreeNode<K, V>> getChildren(Flag... flags);

	Set<Object> getChildrenNames();

	Set<Object> getChildrenNames(Flag... flags);

	Map<K, V> getData();

	Map<K, V> getData(Flag... flags);

	Set<K> getKeys();

	Set<K> getKeys(Flag... flags);

	Fqn getFqn();

	TreeNode<K, V> addChild(Fqn f);

	TreeNode<K, V> addChild(Fqn f, Flag... flags);

	boolean removeChild(Fqn f);

	boolean removeChild(Fqn f, Flag... flags);

	boolean removeChild(Object childName);

	boolean removeChild(Object childName, Flag... flags);

	TreeNode<K, V> getChild(Fqn f);

	TreeNode<K, V> getChild(Fqn f, Flag... flags);

	TreeNode<K, V> getChild(Object name);

	TreeNode<K, V> getChild(Object name, Flag... flags);

	V put(K key, V value);

	V put(K key, V value, Flag... flags);

	V putIfAbsent(K key, V value);

	V putIfAbsent(K key, V value, Flag... flags);

	V replace(K key, V value);

	V replace(K key, V value, Flag... flags);

	boolean replace(K key, V oldValue, V newValue);

	boolean replace(K key, V oldValue, V newValue, Flag... flags);

	void putAll(Map<? extends K, ? extends V> map);

	void putAll(Map<? extends K, ? extends V> map, Flag... flags);

	void replaceAll(Map<? extends K, ? extends V> map);

	void replaceAll(Map<? extends K, ? extends V> map, Flag... flags);

	V get(K key);

	V get(K key, Flag... flags);

	V remove(K key);

	V remove(K key, Flag... flags);

	void clearData();

	void clearData(Flag... flags);

	int dataSize();

	int dataSize(Flag... flags);

	boolean hasChild(Fqn f);

	boolean hasChild(Fqn f, Flag... flags);

	boolean hasChild(Object o);

	boolean hasChild(Object o, Flag... flags);

	boolean isValid();

	void removeChildren();

	void removeChildren(Flag... flags);
}
