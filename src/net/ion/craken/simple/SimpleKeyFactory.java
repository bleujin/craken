package net.ion.craken.simple;

import org.apache.commons.lang.builder.ToStringBuilder;

import net.ion.craken.EntryKey;

public class SimpleKeyFactory {

	public static EmanonKey create(Object genKey) {
		return new EmanonKey(genKey);
	}

}
