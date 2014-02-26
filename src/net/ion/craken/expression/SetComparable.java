package net.ion.craken.expression;

import java.util.Set;

public class SetComparable implements Comparable<SetComparable>{

	private Set<Object> values;
	public SetComparable(Set<Object> values){
		this.values = values ;
	} 
	
	@Override
	public int compareTo(SetComparable o) {
		return 0;
	}
	
	public String toString(){
		return values.toString() ;
	}

	public Set<Object> asSet() {
		return values;
	}
	public Object[] asArray() {
		return values.toArray();
	}
}