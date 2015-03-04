package net.ion.craken.node.crud.util;

public class Pair<K, T> {

	private K key;
	private T val;

	public Pair(K key, T val) {
		this.key = key;
		this.val = val;
	}

	public K key() {
		return key;
	}

	public T val() {
		return val;
	}

}
