package net.ion.craken.simple;


public class SimpleKeyFactory {

	public static EmanonKey create(Object genKey) {
		return new EmanonKey(genKey);
	}

}
