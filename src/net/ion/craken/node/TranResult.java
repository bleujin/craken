package net.ion.craken.node;

import org.apache.commons.lang.builder.ToStringBuilder;

public class TranResult {

	private int count ;
	private long elapsedTime ;
	
	private TranResult(int count, long elapsedTime) {
		this.count = count ;
		this.elapsedTime = elapsedTime ;
	}

	public static TranResult create(int count, long elapsedTime) {
		return new TranResult(count, elapsedTime);
	}
	
	public int count(){
		return count ;
	}
	
	public long elapsedTime(){
		return elapsedTime;
	}
	
	public String toString(){
		return ToStringBuilder.reflectionToString(this) ;
	}

}
